package com.travlejava.ai.service.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具：获取任务执行统计
 */
@Component
public class GetJobStatisticsTool implements AgentTool {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("get_job_statistics");
        definition.setDescription("获取任务执行统计信息，包括总执行次数、成功次数、失败次数和成功率");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> startTimeProp = new HashMap<>();
        startTimeProp.put("type", "string");
        startTimeProp.put("description", "开始时间，格式：yyyy-MM-dd HH:mm:ss");
        properties.put("startTime", startTimeProp);

        Map<String, Object> endTimeProp = new HashMap<>();
        endTimeProp.put("type", "string");
        endTimeProp.put("description", "结束时间，格式：yyyy-MM-dd HH:mm:ss");
        properties.put("endTime", endTimeProp);

        parameters.put("properties", properties);
        definition.setParameters(parameters);

        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            String baseUrl = "http://localhost:8084/api/jobs/statistics";
            StringBuilder url = new StringBuilder(baseUrl);
            boolean hasParam = false;

            if (arguments != null && arguments.containsKey("startTime")) {
                url.append("?startTime=").append(arguments.get("startTime"));
                hasParam = true;
            }
            if (arguments != null && arguments.containsKey("endTime")) {
                url.append(hasParam ? "&" : "?").append("endTime=").append(arguments.get("endTime"));
            }

            String response = restTemplate.getForObject(url.toString(), String.class);
            return new ToolResult("job_stats_1", "get_job_statistics", response, true);
        } catch (Exception e) {
            return new ToolResult("job_stats_1", "get_job_statistics", "获取任务统计失败: " + e.getMessage(), false);
        }
    }
}