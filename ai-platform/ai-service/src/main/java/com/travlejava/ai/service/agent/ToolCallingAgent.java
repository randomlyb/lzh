package com.travlejava.ai.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.dto.AgentSummaryRequest;
import com.travlejava.ai.pojo.agent.dto.AgentSummaryResponse;
import com.travlejava.ai.pojo.agent.tool.ToolCall;
import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatRequest;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatResponse;
import com.travlejava.ai.pojo.llm.deepseek.Message;
import com.travlejava.ai.service.agent.tools.AgentTool;
import com.travlejava.ai.service.rag.llm.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool Calling Agent
 * 支持工具调用的智能Agent
 */
@Component
public class ToolCallingAgent {

    private static final Logger logger = LoggerFactory.getLogger(ToolCallingAgent.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DeepSeekClient deepSeekClient;
    private final Map<String, AgentTool> tools;
    private final List<ToolDefinition> toolDefinitions;

    public ToolCallingAgent(DeepSeekClient deepSeekClient,
                           List<AgentTool> agentTools) {
        this.deepSeekClient = deepSeekClient;
        this.tools = agentTools.stream()
                .collect(Collectors.toMap(
                        tool -> tool.getDefinition().getName(),
                        tool -> tool
                ));
        this.toolDefinitions = agentTools.stream()
                .map(AgentTool::getDefinition)
                .collect(Collectors.toList());
    }

    /**
     * 执行Tool Calling流程
     */
    public Mono<AgentSummaryResponse> execute(AgentSummaryRequest request) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request);

        return chatWithTools(systemPrompt, userPrompt, request.getMaxRetries(), 0)
                .map(response -> parseFinalResponse(response, request));
    }

    private Mono<String> chatWithTools(String systemPrompt, String userPrompt,
                                        int maxRetries, int iteration) {
        if (iteration >= maxRetries) {
            return Mono.just("已达到最大迭代次数，返回当前结果");
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", userPrompt));

        DeepSeekChatRequest chatRequest = new DeepSeekChatRequest();
        chatRequest.setMessages(messages);
        chatRequest.setModel("qwen-plus");
        chatRequest.setTemperature(0.3);
        chatRequest.setMax_tokens(1024);
        chatRequest.setStream(false);
        chatRequest.setTools(toolDefinitions);
        chatRequest.setTool_choice("auto");

        return deepSeekClient.chatCompletion(chatRequest)
                .flatMap(response -> {
                    if (hasToolCalls(response)) {
                        List<ToolCall> toolCalls = extractToolCalls(response);
                        logger.info("检测到{}个工具调用", toolCalls.size());

                        List<Mono<ToolResult>> toolResults = toolCalls.stream()
                                .map(this::executeTool)
                                .collect(Collectors.toList());

                        return Mono.zip(toolResults, results -> {
                            StringBuilder toolResultsText = new StringBuilder();
                            for (Object result : results) {
                                ToolResult toolResult = (ToolResult) result;
                                toolResultsText.append("【")
                                        .append(toolResult.getToolName())
                                        .append("】结果：\n")
                                        .append(toolResult.getResult())
                                        .append("\n\n");
                            }

                            String nextUserPrompt = userPrompt + "\n\n" + toolResultsText.toString();
                            return chatWithTools(systemPrompt, nextUserPrompt, maxRetries, iteration + 1);
                        }).flatMap(mono -> mono);
                    } else {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        return Mono.just(content);
                    }
                });
    }

    private Mono<ToolResult> executeTool(ToolCall toolCall) {
        AgentTool tool = tools.get(toolCall.getFunction().getName());
        if (tool == null) {
            logger.warn("未找到工具: {}", toolCall.getFunction().getName());
            return Mono.just(new ToolResult(toolCall.getId(), toolCall.getFunction().getName(),
                    "工具不存在", false));
        }

        try {
            Map<String, Object> arguments = objectMapper.readValue(
                    toolCall.getFunction().getArguments(), Map.class);
            return Mono.fromCallable(() -> tool.execute(arguments))
                    .onErrorResume(e -> {
                        logger.error("工具执行失败: {}", toolCall.getFunction().getName(), e);
                        return Mono.just(new ToolResult(toolCall.getId(), toolCall.getFunction().getName(),
                                "执行失败：" + e.getMessage(), false));
                    });
        } catch (Exception e) {
            logger.error("解析工具参数失败", e);
            return Mono.just(new ToolResult(toolCall.getId(), toolCall.getFunction().getName(),
                    "参数解析失败", false));
        }
    }

    private boolean hasToolCalls(DeepSeekChatResponse response) {
        return response.getChoices().get(0).getMessage().getTool_calls() != null
                && !response.getChoices().get(0).getMessage().getTool_calls().isEmpty();
    }

    private List<ToolCall> extractToolCalls(DeepSeekChatResponse response) {
        return response.getChoices().get(0).getMessage().getTool_calls();
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