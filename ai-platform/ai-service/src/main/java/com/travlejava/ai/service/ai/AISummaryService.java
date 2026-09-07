package com.travlejava.ai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.ai.dto.SummaryRequest;
import com.travlejava.ai.pojo.ai.dto.SummaryResponse;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatRequest;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatResponse;
import com.travlejava.ai.pojo.llm.deepseek.Message;
import com.travlejava.ai.service.rag.llm.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class AISummaryService {

    private static final Logger logger = LoggerFactory.getLogger(AISummaryService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DeepSeekClient deepSeekClient;

    public AISummaryService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    /**
     * 生成文章摘要和标签
     * 一次模型调用同时生成摘要和标签
     */
    public Mono<SummaryResponse> generateSummaryAndTags(SummaryRequest request) {
        String prompt = buildPrompt(request);

        DeepSeekChatRequest chatRequest = new DeepSeekChatRequest(
                List.of(new Message("user", prompt)),
                "qwen-plus",
                0.3,
                512,
                false
        );

        return deepSeekClient.chatCompletion(chatRequest)
                .map(this::parseResponse);
    }

    /**
     * 同步版本
     */
    public SummaryResponse generateSummaryAndTagsSync(SummaryRequest request) {
        return generateSummaryAndTags(request).block();
    }

    private String buildPrompt(SummaryRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的技术文章分析助手。请根据提供的文章标题（和内容，如果有）完成以下任务：\n\n");
        prompt.append("1. 生成一段简洁的摘要（100字以内）\n");
        prompt.append("2. 提取3-5个关键词标签\n\n");
        prompt.append("请严格按照以下JSON格式返回结果：\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"文章摘要内容\",\n");
        prompt.append("  \"tags\": [\"标签1\", \"标签2\", \"标签3\"]\n");
        prompt.append("}\n\n");
        prompt.append("文章标题：").append(request.getTitle()).append("\n");
        
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            prompt.append("文章内容：").append(request.getContent()).append("\n");
        }
        
        prompt.append("\n请直接返回JSON，不要包含其他解释文字。");
        
        return prompt.toString();
    }

    private SummaryResponse parseResponse(DeepSeekChatResponse response) {
        try {
            String content = response.getChoices().get(0).getMessage().getContent();
            
            // 尝试从响应中提取JSON
            String jsonStr = extractJson(content);
            JsonNode node = objectMapper.readTree(jsonStr);
            
            String summary = node.has("summary") ? node.get("summary").asText() : "";
            List<String> tags = new ArrayList<>();
            
            if (node.has("tags") && node.get("tags").isArray()) {
                node.get("tags").forEach(tag -> tags.add(tag.asText()));
            }
            
            return new SummaryResponse(summary, tags);
        } catch (Exception e) {
            logger.error("解析AI响应失败", e);
            return new SummaryResponse("", List.of());
        }
    }

    /**
     * 从响应中提取JSON字符串
     */
    private String extractJson(String content) {
        // 尝试直接解析
        if (content.trim().startsWith("{")) {
            return content.trim();
        }
        
        // 尝试从markdown代码块中提取
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
        
        // 尝试找到第一个{和最后一个}
        int firstBrace = content.indexOf("{");
        int lastBrace = content.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }
        
        return content.trim();
    }
}