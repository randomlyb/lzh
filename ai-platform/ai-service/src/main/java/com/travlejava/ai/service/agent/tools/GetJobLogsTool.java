package com.travlejava.ai.service.agent.tools;

import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 工具：获取任务执行日志
 */
@Component
public class GetJobLogsTool implements AgentTool {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("get_job_logs");
        definition.setDescription("获取指定任务的执行日志，包括执行时间、状态、耗时、错误信息等。用于深入分析具体任务的执行情况。");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> jobIdProp = new HashMap<>();
        jobIdProp.put("type", "string");
        jobIdProp.put("description", "任务ID（可选，不传则返回所有任务的日志）");
        properties.put("jobId", jobIdProp);

        Map<String, Object> statusProp = new HashMap<>();
        statusProp.put("type", "string");
        statusProp.put("description", "执行状态过滤（可选）：SUCCESS-成功, FAILED-失败, RUNNING-运行中");
        properties.put("status", statusProp);

        Map<String, Object> limitProp = new HashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "返回日志条数限制（默认50条）");
        properties.put("limit", limitProp);

        parameters.put("properties", properties);
        definition.setParameters(parameters);

        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            StringBuilder url = new StringBuilder("http://localhost:8084/api/job-logs");
            boolean hasParam = false;

            if (arguments != null) {
                if (arguments.containsKey("jobId")) {
                    url.append("?jobId=").append(arguments.get("jobId"));
                    hasParam = true;
                }
                if (arguments.containsKey("status")) {
                    url.append(hasParam ? "&" : "?").append("status=").append(arguments.get("status"));
                    hasParam = true;
                }
                if (arguments.containsKey("limit")) {
                    url.append(hasParam ? "&" : "?").append("limit=").append(arguments.get("limit"));
                }
            }

            String response = restTemplate.getForObject(url.toString(), String.class);
            return new ToolResult("job_logs_1", "get_job_logs", response, true);
        } catch (Exception e) {
            return new ToolResult("job_logs_1", "get_job_logs", "获取任务日志失败: " + e.getMessage(), false);
        }
    }
}