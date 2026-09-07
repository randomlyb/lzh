package com.travlejava.ai.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.dto.AgentSummaryRequest;
import com.travlejava.ai.pojo.agent.dto.AgentSummaryResponse;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatRequest;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatResponse;
import com.travlejava.ai.pojo.llm.deepseek.Message;
import com.travlejava.ai.service.rag.llm.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent编排器
 * 负责协调分类、策略路由、LLM调用、质量评估和重试
 */
@Component
public class SummaryAgent {

    private static final Logger logger = LoggerFactory.getLogger(SummaryAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DeepSeekClient deepSeekClient;
    private final ArticleClassifier classifier;
    private final StrategyRouter strategyRouter;
    private final QualityEvaluator evaluator;

    public SummaryAgent(DeepSeekClient deepSeekClient,
                       ArticleClassifier classifier,
                       StrategyRouter strategyRouter,
                       QualityEvaluator evaluator) {
        this.deepSeekClient = deepSeekClient;
        this.classifier = classifier;
        this.strategyRouter = strategyRouter;
        this.evaluator = evaluator;
    }

    /**
     * 执行Agent摘要生成流程
     */
    public Mono<AgentSummaryResponse> execute(AgentSummaryRequest request) {
        // 1. 分类文章
        String category = classifier.classify(request.getTitle(), request.getContent());
        logger.info("文章分类结果: {}", category);

        // 2. 获取策略模板
        String promptTemplate = strategyRouter.getPromptTemplate(category);
        String prompt = String.format(promptTemplate, request.getTitle());

        // 3. 调用LLM并评估
        return callLLMWithRetry(prompt, request.getMaxRetries(), category, 0);
    }

    private Mono<AgentSummaryResponse> callLLMWithRetry(String prompt, int maxRetries,
                                                         String category, int retryCount) {
        DeepSeekChatRequest chatRequest = new DeepSeekChatRequest(
                List.of(new Message("user", prompt)),
                "qwen-plus",
                0.3,
                512,
                false
        );

        return deepSeekClient.chatCompletion(chatRequest)
                .map(response -> parseAndEvaluate(response, category, retryCount))
                .flatMap(result -> {
                    int score = result.getQualityScore();
                    if (evaluator.needsRetry(score) && retryCount < maxRetries) {
                        logger.info("质量分数{}低于阈值，进行第{}次重试", score, retryCount + 1);
                        // 重试时增加temperature提高创造性
                        return callLLMWithRetry(prompt, maxRetries, category, retryCount + 1);
                    }
                    return Mono.just(result);
                });
    }

    private AgentSummaryResponse parseAndEvaluate(DeepSeekChatResponse response,
                                                   String category, int retryCount) {
        try {
            String content = response.getChoices().get(0).getMessage().getContent();
            String jsonStr = extractJson(content);
            JsonNode node = objectMapper.readTree(jsonStr);

            String summary = node.has("summary") ? node.get("summary").asText() : "";
            List<String> tags = new ArrayList<>();

            if (node.has("tags") && node.get("tags").isArray()) {
                node.get("tags").forEach(tag -> tags.add(tag.asText()));
            }

            AgentSummaryResponse result = new AgentSummaryResponse();
            result.setSummary(summary);
            result.setTags(tags);
            result.setCategory(category);
            result.setStrategy(getStrategyName(category));
            result.setRetryCount(retryCount);

            // 评估质量
            int score = evaluator.evaluate(result);
            result.setQualityScore(score);

            logger.info("摘要生成完成 - 分类:{}, 策略:{}, 重试:{}, 质量分数:{}",
                    category, result.getStrategy(), retryCount, score);

            return result;
        } catch (Exception e) {
            logger.error("解析AI响应失败", e);
            AgentSummaryResponse fallback = new AgentSummaryResponse();
            fallback.setSummary("");
            fallback.setTags(List.of());
            fallback.setCategory(category);
            fallback.setStrategy(getStrategyName(category));
            fallback.setRetryCount(retryCount);
            fallback.setQualityScore(0);
            return fallback;
        }
    }

    private String getStrategyName(String category) {
        return switch (category) {
            case "java" -> "java-specialist";
            case "ai" -> "ai-specialist";
            case "database" -> "database-specialist";
            case "frontend" -> "frontend-specialist";
            case "cloud" -> "cloud-specialist";
            default -> "general-specialist";
        };
    }

    private String extractJson(String content) {
        if (content.trim().startsWith("{")) {
            return content.trim();
        }

        int start = content.indexOf("```json");
        if (start != -1) {
            int end = content.indexOf("```", start + 7);
            if (end != -1) {
                return content.substring(start + 7, end).trim();
            }
        }

        start = content.indexOf("```");
        if (start != -1) {
            int end = content.indexOf("```", start + 3);
            if (end != -1) {
                return content.substring(start + 3, end).trim();
            }
        }

        int firstBrace = content.indexOf("{");
        int lastBrace = content.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }

        return content.trim();
    }
}