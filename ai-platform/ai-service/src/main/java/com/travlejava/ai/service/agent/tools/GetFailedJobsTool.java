package com.travlejava.ai.service.agent.tools;

import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 工具：获取失败任务详情
 */
@Component
public class GetFailedJobsTool implements AgentTool {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("get_failed_jobs");
        definition.setDescription("获取失败任务的详细信息，包括任务名称、失败次数、最近错误信息和平均耗时");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        definition.setParameters(parameters);

        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            String url = "http://localhost:8084/api/jobs/failed";
            String response = restTemplate.getForObject(url, String.class);
            return new ToolResult("failed_jobs_1", "get_failed_jobs", response, true);
        } catch (Exception e) {
            return new ToolResult("failed_jobs_1", "get_failed_jobs", "获取失败任务失败: " + e.getMessage(), false);
        }
    }
}