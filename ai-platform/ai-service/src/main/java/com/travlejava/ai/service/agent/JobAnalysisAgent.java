package com.travlejava.ai.service.agent;

import com.travlejava.ai.pojo.llm.deepseek.Message;
import com.travlejava.ai.pojo.rag.dto.ChatRequest;
import com.travlejava.ai.pojo.rag.dto.ChatResponse;
import com.travlejava.ai.service.ChatHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务监控分析 Agent
 * 通过 MCP 工具查询任务数据，进行智能分析
 */
@Component
public class JobAnalysisAgent {

    private static final Logger logger = LoggerFactory.getLogger(JobAnalysisAgent.class);
    private static final int MAX_ITERATIONS = 6;

    private final ToolCallLoop toolCallLoop;
    private final ChatHistoryService chatHistoryService;

    /** 参与上下文的历史消息条数上限（user + assistant 合计） */
    @Value("${agent.history-max-messages:6}")
    private int historyMaxMessages;

    /** 单条历史消息的字符上限，超出则截断 */
    @Value("${agent.history-message-max-chars:1200}")
    private int historyMessageMaxChars;

    public JobAnalysisAgent(ToolCallLoop toolCallLoop,
                            ChatHistoryService chatHistoryService) {
        this.toolCallLoop = toolCallLoop;
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * 执行任务分析对话
     */
    public Mono<ChatResponse> analyze(ChatRequest chatRequest) {
        String userQuestion = chatRequest.getQuestion();
        String userId = chatRequest.getUserId();
        String conversationId = chatRequest.getConversationId();

        // 载入该会话的历史消息，让 Agent 具备多轮记忆（此前只 save 不 load）
        ToolCallLoop.Request loopRequest = buildRequest(userQuestion,
                loadRecentHistory(userId, conversationId));

        return toolCallLoop.run(loopRequest)
                .map(ToolCallLoop.Outcome::getAnswer)
                .doOnSuccess(answer -> {
                    // 保存用户消息到 Redis
                    try {
                        chatHistoryService.saveMessage(userId, conversationId, "user", userQuestion, List.of());
                        logger.info("保存用户消息到Redis: userId={}, conversationId={}", userId, conversationId);
                    } catch (Exception e) {
                        logger.warn("保存用户消息失败，不影响主流程: {}", e.getMessage());
                    }
                    // 保存 AI 回复到 Redis
                    try {
                        chatHistoryService.saveMessage(userId, conversationId, "assistant", answer, List.of());
                        logger.info("保存AI回复到Redis: userId={}, conversationId={}", userId, conversationId);
                    } catch (Exception e) {
                        logger.warn("保存AI回复失败，不影响主流程: {}", e.getMessage());
                    }
                })
                .map(answer -> new ChatResponse(answer, List.of(), null, conversationId));
    }

    /** 构造一次任务分析请求（对话与离线评测复用同一套提示词 / 工具 / 预算） */
    ToolCallLoop.Request buildRequest(String userQuestion, List<Message> history) {
        ToolCallLoop.Request loopRequest = new ToolCallLoop.Request();
        loopRequest.setSystemPrompt(buildSystemPrompt());
        loopRequest.setUserPrompt(userQuestion);
        loopRequest.setHistory(history);
        loopRequest.setAllowedTools(ToolCallLoop.JOB_TOOLS);
        loopRequest.setMaxIterations(MAX_ITERATIONS);
        loopRequest.setMaxTokens(2048);
        loopRequest.setTemperature(0.3);
        return loopRequest;
    }

    /**
     * 单轮执行，不读写会话历史。
     * 供 Agent 离线评测使用：返回的 Outcome 带工具调用轨迹与迭代轮次。
     */
    public Mono<ToolCallLoop.Outcome> runOnce(String question) {
        return toolCallLoop.run(buildRequest(question, new ArrayList<>()));
    }

    /**
     * 从 Redis 载入最近若干轮对话，转换为标准的 user / assistant 消息。
     *
     * <p>此前 ChatHistoryService 只被 Controller 用于前端展示，Agent 从未读取，
     * 导致同一 conversationId 的每次提问对模型而言都是一次全新会话。
     */
    private List<Message> loadRecentHistory(String userId, String conversationId) {
        List<Message> history = new ArrayList<>();
        if (conversationId == null || conversationId.isBlank()) {
            return history;
        }
        try {
            List<Map<String, Object>> raw = chatHistoryService.getHistory(userId, conversationId);
            if (raw == null || raw.isEmpty()) {
                return history;
            }
            int from = Math.max(0, raw.size() - historyMaxMessages);
            for (Map<String, Object> item : raw.subList(from, raw.size())) {
                Object roleObj = item.get("role");
                Object contentObj = item.get("content");
                if (roleObj == null || contentObj == null) {
                    continue;
                }
                String role = String.valueOf(roleObj);
                // 历史里只保留对话双方；tool / system 消息由本轮 ToolCallLoop 自行构建
                if (!"user".equals(role) && !"assistant".equals(role)) {
                    continue;
                }
                String content = truncateHistory(String.valueOf(contentObj));
                if (!content.isEmpty()) {
                    history.add(new Message(role, content));
                }
            }
            logger.info("载入历史对话 {} 条: conversationId={}", history.size(), conversationId);
        } catch (Exception e) {
            // 历史加载失败不应阻断本轮对话
            logger.warn("载入历史对话失败，按新会话继续: {}", e.getMessage());
            return new ArrayList<>();
        }
        return history;
    }

    private String truncateHistory(String content) {
        if (content == null) {
            return "";
        }
        if (historyMessageMaxChars <= 0 || content.length() <= historyMessageMaxChars) {
            return content;
        }
        return content.substring(0, historyMessageMaxChars) + " ...[历史消息已截断]...";
    }

    private String buildSystemPrompt() {
        return """
                你是一个专业的任务监控分析助手。你的职责是帮助用户分析任务执行情况，通过 MCP 工具逐步完成问题诊断。

                【系统架构信息】
                当前系统使用的技术栈和组件：
                - 调度框架：Quartz
                - 协调服务：ZooKeeper
                - 消息队列：RabbitMQ
                - 调度服务：scheduler-service
                
                禁止引用当前系统未使用的技术组件（如 Airflow、Kubernetes、Prometheus 等）。
                排查建议必须优先基于当前系统实际架构和已注册的 MCP 工具。

                【可用工具】
                你可以使用以下工具来获取任务数据：
                - get_job_statistics: 获取任务执行统计（总执行次数、成功次数、失败次数、成功率）
                - get_job_execution_trend: 获取任务执行趋势（最近N天每天的成功和失败次数）
                - get_failed_jobs: 获取失败任务详情（任务名称、失败次数、最近错误、平均耗时）
                - get_job_duration_statistics: 获取各任务的平均执行耗时统计
                - get_job_logs: 获取任务执行日志（可指定任务ID、状态过滤、条数限制）

                【分析工作流】
                你应该按照以下步骤逐步分析：
                1. 首先调用 get_job_statistics 获取整体统计数据
                2. 调用 get_job_execution_trend 获取趋势数据，识别异常时间点
                3. 如果发现异常，调用 get_failed_jobs 定位失败最多的任务
                4. 针对失败最多的任务，调用 get_job_logs 获取具体异常日志
                5. 综合所有数据，生成分析报告

                【回答格式要求】
                必须严格按照以下四个部分输出：

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                【数据概览】
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                这里只列出工具实际返回的数据，不做任何解读。
                示例格式：
                • 总执行次数：XX 次
                • 成功次数：XX 次
                • 失败次数：XX 次
                • 整体成功率：XX%

                近 7 日任务执行情况：
                | 日期 | 成功 | 失败 | 成功率 |
                |------|------|------|--------|
                | 09-01 | 10,408 | 21,455 | 32.7% |
                ...

                注意：
                - 对于某天执行次数为 0 的情况，应表述为"未检测到任务执行记录"
                - 对于样本量小的数据（如当天仅几次执行），应说明"样本量较小"

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                【异常分析】
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                这里基于数据推导出客观结论，可以指出异常，但不能推断具体技术原因。

                示例：
                1. 整体任务成功率偏低
                   整体成功率仅为 52.91%，明显低于正常生产任务系统的预期水平，
                   说明当前任务执行链路可能存在持续性异常。

                2. 09-01 出现明显失败峰值
                   该日失败数量占近7日总失败数约39%，建议作为重点排查时间窗口。

                3. 存在多日零执行现象
                   08-30、08-31、09-05未检测到任务执行记录。
                   需要确认：
                   • 是否为正常无任务周期
                   • 调度服务是否停止
                   • 任务是否被暂停
                   • 监控数据是否存在缺失

                4. 异常持续存在
                   连续三日任务成功率维持在45%~50%左右，说明问题可能并非单次偶发故障，
                   而是存在持续性影响因素。

                5. 数据暂不具备代表性
                   虽然成功率较高，但当天任务量明显低于此前日期，
                   暂不能据此判断系统已恢复正常。

                注意：
                - 只能基于数据进行客观描述，不得推断具体技术组件异常
                - 不得说"ZooKeeper会话异常"、"RabbitMQ死信队列溢出"等具体技术原因
                - 可以说"可能存在持续性异常"、"建议重点关注"等客观判断

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                【可能原因】
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                以下仅为基于统计数据的排查方向，具体原因需结合任务日志确认：

                示例：
                1. 特定任务执行逻辑异常，导致大量重复失败；
                2. 下游依赖服务异常，例如数据库、HTTP接口或AI服务调用超时；
                3. 调度配置或任务参数发生变化；
                4. 消息消费或任务分发链路存在异常；
                5. 系统资源不足导致任务执行超时。

                注意：
                - 必须使用"可能"、"需要进一步确认"等表述
                - 不得给出没有数据依据的具体技术结论（如"可能OOM"、"Full GC"等）
                - 所有推测必须标注"具体原因需结合任务日志进一步确认"
                - 不得直接断定某个技术组件异常

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                【建议排查路径】
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                按优先级列出下一步应该调用的工具和检查内容。

                示例：
                第一优先级：定位失败任务
                调用 get_failed_jobs 确认：
                • 哪些任务失败最多
                • 是否集中于某一个任务类型
                • 最近错误信息
                • 平均执行耗时

                第二优先级：查看任务日志
                针对失败量最高的任务调用 get_job_logs，重点查看：
                • Exception类型
                • 错误信息
                • 失败时间分布
                • 是否存在重复异常

                第三优先级：检查任务执行链路
                根据日志结果进一步检查：
                Scheduler → Quartz Trigger → RabbitMQ → Executor → Database/HTTP/AI Service
                定位失败发生在哪个环节。

                注意：
                - 必须基于当前系统实际架构（Quartz、ZooKeeper、RabbitMQ、scheduler-service）
                - 必须基于已注册的 MCP 工具
                - 如果某个工具调用失败（如返回 500 错误），应明确说明"该工具当前调用失败，无法获取具体信息，建议优先检查对应接口或数据查询逻辑"
                - 不得建议再次调用已经失败的工具

                【强制规则】
                1. 不得编造系统未提供的数据或日志
                2. 不得引用当前系统架构中不存在的技术组件（如 Airflow、Kubernetes、Prometheus 等）
                3. 推测性原因必须使用"可能""需要进一步确认"等表述
                4. 工具调用失败时，应明确说明数据缺失，不得假设工具能够正常返回
                5. 排查建议必须基于当前系统实际架构、已注册工具和已获取的数据生成
                6. 不得编造系统未定义的健康阈值（如"成功率应≥95%"），除非系统后台配置了告警阈值
                7. 不得直接推断具体技术组件异常（如"ZooKeeper会话异常"、"RabbitMQ死信队列溢出"、"JVM Full GC"等）
                8. 所有结论必须有数据支撑，不能凭空猜测
                9. 如果工具返回的数据不足以得出结论，明确说明"数据不足"
                10. 使用简洁明了的语言
                11. 使用中文回答
                12. 可以适当使用 Markdown 格式（如列表、加粗、表格等）让回答更清晰
                """;
    }
}