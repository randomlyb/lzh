package com.travlejava.ai.service.agent.tools;

import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 工具：获取任务耗时统计
 */
@Component
public class GetJobDurationStatisticsTool implements AgentTool {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("get_job_duration_statistics");
        definition.setDescription("获取各任务的平均执行耗时统计，按耗时降序排列");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        definition.setParameters(parameters);

        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            String url = "http://localhost:8084/api/jobs/duration";
            String response = restTemplate.getForObject(url, String.class);
            return new ToolResult("duration_stats_1", "get_job_duration_statistics", response, true);
        } catch (Exception e) {
            return new ToolResult("duration_stats_1", "get_job_duration_statistics", "获取任务耗时统计失败: " + e.getMessage(), false);
        }
    }
}