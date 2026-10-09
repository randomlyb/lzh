package com.travlejava.ai.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.dto.AgentSummaryRequest;
import com.travlejava.ai.pojo.agent.dto.AgentSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool Calling Agent
 * 支持工具调用的智能 Agent。
 *
 * <p>工具循环（消息协议、轮次预算、上下文压缩）统一由 {@link ToolCallLoop} 承载，
 * 本类只负责提示词与结果解析。
 */
@Component
public class ToolCallingAgent {

    private static final Logger logger = LoggerFactory.getLogger(ToolCallingAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_MAX_ITERATIONS = 4;

    private final ToolCallLoop toolCallLoop;

    public ToolCallingAgent(ToolCallLoop toolCallLoop) {
        this.toolCallLoop = toolCallLoop;
    }

    /**
     * 执行 Tool Calling 流程
     */
    public Mono<AgentSummaryResponse> execute(AgentSummaryRequest request) {
        ToolCallLoop.Request loopRequest = new ToolCallLoop.Request();
        loopRequest.setSystemPrompt(buildSystemPrompt());
        loopRequest.setUserPrompt(buildUserPrompt(request));
        loopRequest.setAllowedTools(ToolCallLoop.ARTICLE_TOOLS);
        loopRequest.setMaxIterations(request.getMaxRetries() != null
                ? request.getMaxRetries() : DEFAULT_MAX_ITERATIONS);
        loopRequest.setMaxTokens(1024);
        loopRequest.setTemperature(0.3);

        return toolCallLoop.run(loopRequest).map(outcome -> {
            logger.info("Tool Calling Agent 完成: 迭代 {} 轮, 工具轨迹 {}, 预算耗尽={}",
                    outcome.getIterations(), outcome.getToolTrace(), outcome.isBudgetExhausted());
            AgentSummaryResponse response = parseFinalResponse(outcome.getAnswer(), request);
            response.setRetryCount(outcome.getIterations());
            return response;
        });
    }

    private AgentSummaryResponse parseFinalResponse(String content, AgentSummaryRequest request) {
        try {
            String jsonStr = extractJson(content);
            JsonNode node = objectMapper.readTree(jsonStr);

            String summary = node.has("summary") ? node.get("summary").asText() : "";
            List<String> tags = new ArrayList<>();

            if (node.has("tags") && node.get("tags").isArray()) {
                node.get("tags").forEach(tag -> tags.add(tag.asText()));
            }

            AgentSummaryResponse response = new AgentSummaryResponse();
            response.setSummary(summary);
            response.setTags(tags);
            response.setCategory("tool-calling");
            response.setStrategy("tool-calling-agent");
            response.setRetryCount(0);
            response.setQualityScore(90);

            return response;
        } catch (Exception e) {
            logger.error("解析最终响应失败", e);
            AgentSummaryResponse fallback = new AgentSummaryResponse();
            fallback.setSummary("");
            fallback.setTags(List.of());
            fallback.setCategory("tool-calling");
            fallback.setStrategy("tool-calling-agent");
            fallback.setRetryCount(0);
            fallback.setQualityScore(0);
            return fallback;
        }
    }

    private String buildSystemPrompt() {
        return """
                你是一个智能文章分析助手，可以使用以下工具来分析文章：

                1. search_article - 搜索相关文章获取上下文
                2. query_knowledge - 查询特定领域的专业知识
                3. generate_summary - 生成文章摘要
                4. extract_tags - 提取关键词标签

                请根据需要使用这些工具，最终返回JSON格式的结果：
                {
                  "summary": "文章摘要",
                  "tags": ["标签1", "标签2", "标签3"]
                }
                """;
    }

    private String buildUserPrompt(AgentSummaryRequest request) {
        return String.format("请分析以下文章并生成摘要和标签：\n标题：%s", request.getTitle());
    }

    private String extractJson(String content) {
        if (content == null) {
            return "{}";
        }
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

        int firstBrace = content.indexOf("{");
        int lastBrace = content.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }

        return content.trim();
    }
}
