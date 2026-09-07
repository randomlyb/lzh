package com.travlejava.ai.service.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 工具：获取任务执行趋势
 */
@Component
public class GetJobExecutionTrendTool implements AgentTool {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("get_job_execution_trend");
        definition.setDescription("获取任务执行趋势数据，返回最近N天每天的成功和失败次数");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> daysProp = new HashMap<>();
        daysProp.put("type", "integer");
        daysProp.put("description", "查询天数，默认7天");
        properties.put("days", daysProp);

        parameters.put("properties", properties);
        definition.setParameters(parameters);

        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            int days = 7;
            if (arguments != null && arguments.containsKey("days")) {
                days = ((Number) arguments.get("days")).intValue();
            }

            String url = "http://localhost:8084/api/jobs/trend?days=" + days;
            String response = restTemplate.getForObject(url, String.class);
            return new ToolResult("job_trend_1", "get_job_execution_trend", response, true);
        } catch (Exception e) {
            return new ToolResult("job_trend_1", "get_job_execution_trend", "获取任务趋势失败: " + e.getMessage(), false);
        }
    }
}