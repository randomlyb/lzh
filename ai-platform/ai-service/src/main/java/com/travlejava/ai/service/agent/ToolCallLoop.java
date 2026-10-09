package com.travlejava.ai.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.tool.ToolCall;
import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatRequest;
import com.travlejava.ai.pojo.llm.deepseek.Message;
import com.travlejava.ai.service.agent.tools.AgentTool;
import com.travlejava.ai.service.rag.llm.DeepSeekClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用 ReAct（Reason + Act）工具调用循环，供各 Agent 复用。
 *
 * <p>相对早期实现修复的四个问题：
 * <ol>
 *   <li>工具结果走标准协议：以 role=tool + tool_call_id 的消息回填，
 *       不再把结果拼成文本追加到 user prompt；</li>
 *   <li>上下文完整：助手带 tool_calls 的消息原样进入历史，多轮链路可追溯；</li>
 *   <li>预算耗尽时收敛：用尽轮次后移除工具并强制模型基于已有观测给出答案，
 *       而不是返回一句“已达到最大迭代次数”；</li>
 *   <li>上下文压缩：总字符数超阈值时，从最旧的工具结果开始做头尾保留式压缩，
 *       避免历史随轮次线性膨胀。</li>
 * </ol>
 */
@Component
public class ToolCallLoop {

    private static final Logger logger = LoggerFactory.getLogger(ToolCallLoop.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 任务监控分析 Agent 可用的工具集合 */
    public static final Set<String> JOB_TOOLS = Set.of(
            "get_job_statistics", "get_job_execution_trend", "get_failed_jobs",
            "get_job_duration_statistics", "get_job_logs");

    /** 文章摘要 Agent 可用的工具集合 */
    public static final Set<String> ARTICLE_TOOLS = Set.of(
            "search_article", "query_knowledge", "generate_summary", "extract_tags");

    private static final String CONVERGE_INSTRUCTION =
            "已达到最大工具调用轮次。请立即基于以上已经获取到的数据给出最终回答，不要再调用任何工具。";

    private final DeepSeekClient deepSeekClient;
    private final Map<String, AgentTool> toolRegistry;
    private final List<ToolDefinition> allDefinitions;

    @Value("${agent.model:deepseek-chat}")
    private String model;

    /** 消息总字符预算，超出后触发压缩 */
    @Value("${agent.context-max-chars:24000}")
    private int contextMaxChars;

    /** 单条工具结果落库前的最大字符数 */
    @Value("${agent.tool-result-max-chars:4000}")
    private int toolResultMaxChars;

    @Value("${agent.compress-head-chars:400}")
    private int compressHeadChars;

    @Value("${agent.compress-tail-chars:200}")
    private int compressTailChars;

    public ToolCallLoop(DeepSeekClient deepSeekClient, List<AgentTool> agentTools) {
        this.deepSeekClient = deepSeekClient;
        this.toolRegistry = agentTools.stream().collect(Collectors.toMap(
                t -> t.getDefinition().getName(), t -> t, (a, b) -> a, LinkedHashMap::new));
        this.allDefinitions = agentTools.stream().map(AgentTool::getDefinition).collect(Collectors.toList());
    }

    /** 循环入参 */
    @Data
    public static class Request {
        private String systemPrompt;
        private List<Message> history = new ArrayList<>();
        private String userPrompt;
        /** 允许暴露给模型的工具名；为空表示全部暴露 */
        private Set<String> allowedTools;
        private int maxIterations = 6;
        private int maxTokens = 2048;
        private double temperature = 0.3;
    }

    /** 循环结果 */
    @Data
    @AllArgsConstructor
    public static class Outcome {
        private String answer;
        private int iterations;
        private List<String> toolTrace;
        private boolean budgetExhausted;
    }

    public Mono<Outcome> run(Request request) {
        List<Message> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null) {
            messages.add(new Message("system", request.getSystemPrompt()));
        }
        if (request.getHistory() != null) {
            messages.addAll(request.getHistory());
        }
        messages.add(new Message("user", request.getUserPrompt()));

        List<ToolDefinition> definitions = filterDefinitions(request.getAllowedTools());
        logger.info("ReAct 循环启动: 可用工具 {} 个, 最大轮次 {}",
                definitions.size(), request.getMaxIterations());
        return step(messages, definitions, request, 0, new ArrayList<>());
    }

    private Mono<Outcome> step(List<Message> messages, List<ToolDefinition> definitions,
                               Request request, int iteration, List<String> trace) {
        maybeCompress(messages);

        DeepSeekChatRequest chatRequest = new DeepSeekChatRequest();
        chatRequest.setMessages(new ArrayList<>(messages));
        chatRequest.setModel(model);
        chatRequest.setTemperature(request.getTemperature());
        chatRequest.setMax_tokens(request.getMaxTokens());
        chatRequest.setStream(false);
        if (!definitions.isEmpty()) {
            chatRequest.setTools(definitions);
            chatRequest.setTool_choice("auto");
        }

        return deepSeekClient.chatCompletion(chatRequest).flatMap(response -> {
            Message raw = response.getChoices().get(0).getMessage();
            List<ToolCall> toolCalls = raw.getTool_calls();

            // 没有工具调用 => 模型给出最终答案，循环结束
            if (toolCalls == null || toolCalls.isEmpty()) {
                return Mono.just(new Outcome(raw.getContent(), iteration, trace, false));
            }

            // 助手消息（含 tool_calls）必须原样进入历史，否则 tool 消息无法对应
            messages.add(new Message("assistant", raw.getContent(), toolCalls, null));
            logger.info("第 {} 轮: 模型请求调用 {} 个工具", iteration + 1, toolCalls.size());

            // 预算用尽：不再执行工具，转入强制收敛
            if (iteration >= request.getMaxIterations()) {
                logger.warn("工具调用轮次已达上限 {}，转入强制收敛", request.getMaxIterations());
                return converge(messages, request, iteration, trace);
            }

            return Flux.fromIterable(toolCalls)
                    .concatMap(this::executeTool)
                    .collectList()
                    .flatMap(results -> {
                        for (ToolResult result : results) {
                            messages.add(new Message("tool", truncate(result.getResult()), null,
                                    result.getToolCallId()));
                            trace.add(result.getToolName());
                        }
                        return step(messages, definitions, request, iteration + 1, trace);
                    });
        });
    }

    /** 预算用尽：移除工具，让模型基于已有观测收敛出最终答案 */
    private Mono<Outcome> converge(List<Message> messages, Request request,
                                   int iteration, List<String> trace) {
        messages.add(new Message("user", CONVERGE_INSTRUCTION));
        DeepSeekChatRequest chatRequest = new DeepSeekChatRequest();
        chatRequest.setMessages(new ArrayList<>(messages));
        chatRequest.setModel(model);
        chatRequest.setTemperature(request.getTemperature());
        chatRequest.setMax_tokens(request.getMaxTokens());
        chatRequest.setStream(false);
        // 不设置 tools => 模型无法再发起工具调用
        return deepSeekClient.chatCompletion(chatRequest)
                .map(response -> new Outcome(
                        response.getChoices().get(0).getMessage().getContent(),
                        iteration, trace, true));
    }

    private Mono<ToolResult> executeTool(ToolCall toolCall) {
        String name = toolCall.getFunction() == null ? null : toolCall.getFunction().getName();
        AgentTool tool = name == null ? null : toolRegistry.get(name);
        if (tool == null) {
            logger.warn("模型请求了未注册的工具: {}", name);
            return Mono.just(new ToolResult(toolCall.getId(), name, "工具不存在: " + name, false));
        }
        try {
            Map<String, Object> arguments = objectMapper.readValue(
                    toolCall.getFunction().getArguments(), Map.class);
            return Mono.fromCallable(() -> tool.execute(arguments))
                    // 部分工具内部写死了 toolCallId，这里强制对齐真实调用 id
                    .map(result -> {
                        result.setToolCallId(toolCall.getId());
                        result.setToolName(name);
                        return result;
                    })
                    .onErrorResume(e -> {
                        logger.error("工具执行失败: {}", name, e);
                        return Mono.just(new ToolResult(toolCall.getId(), name,
                                "执行失败：" + e.getMessage(), false));
                    });
        } catch (Exception e) {
            logger.error("工具参数解析失败: {}", name, e);
            return Mono.just(new ToolResult(toolCall.getId(), name, "参数解析失败", false));
        }
    }

    /** 按允许列表过滤工具定义；未配置则全部暴露 */
    private List<ToolDefinition> filterDefinitions(Set<String> allowed) {
        if (allowed == null || allowed.isEmpty()) {
            return allDefinitions;
        }
        return allDefinitions.stream()
                .filter(d -> allowed.contains(d.getName()))
                .collect(Collectors.toList());
    }

    /** 单条工具结果截断，防止一次返回把上下文撑爆 */
    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= toolResultMaxChars) {
            return content;
        }
        int half = toolResultMaxChars / 2;
        return content.substring(0, half)
                + "\n...[工具结果已截断，共 " + content.length() + " 字符]...\n"
                + content.substring(content.length() - half);
    }

    /**
     * 上下文超预算时压缩：只动 role=tool 的消息，且保留最近一条工具结果不动，
     * system / user / assistant 的消息一律不压缩。
     */
    private void maybeCompress(List<Message> messages) {
        int total = totalChars(messages);
        if (total <= contextMaxChars) {
            return;
        }
        int lastToolIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("tool".equals(messages.get(i).getRole())) {
                lastToolIndex = i;
                break;
            }
        }
        int before = total;
        for (int i = 0; i < messages.size() && total > contextMaxChars; i++) {
            if (i == lastToolIndex) {
                continue;
            }
            Message m = messages.get(i);
            if (!"tool".equals(m.getRole()) || m.getContent() == null) {
                continue;
            }
            String compressed = compress(m.getContent());
            if (compressed.length() < m.getContent().length()) {
                total -= (m.getContent().length() - compressed.length());
                m.setContent(compressed);
            }
        }
        logger.info("上下文压缩: {} -> {} 字符 (预算 {})", before, total, contextMaxChars);
    }

    private String compress(String content) {
        int keep = compressHeadChars + compressTailChars;
        if (content.length() <= keep + 40) {
            return content;
        }
        int omitted = content.length() - keep;
        return content.substring(0, compressHeadChars)
                + "\n...[上下文压缩：省略 " + omitted + " 字符]...\n"
                + content.substring(content.length() - compressTailChars);
    }

    private int totalChars(List<Message> messages) {
        int total = 0;
        for (Message m : messages) {
            if (m.getContent() != null) {
                total += m.getContent().length();
            }
            if (m.getTool_calls() != null) {
                for (ToolCall c : m.getTool_calls()) {
                    if (c.getFunction() != null && c.getFunction().getArguments() != null) {
                        total += c.getFunction().getArguments().length();
                    }
                }
            }
        }
        return total;
    }
}
