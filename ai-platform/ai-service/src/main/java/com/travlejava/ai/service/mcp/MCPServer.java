package com.travlejava.ai.service.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.mcp.MCPError;
import com.travlejava.ai.pojo.mcp.MCPRequest;
import com.travlejava.ai.pojo.mcp.MCPResponse;
import com.travlejava.ai.service.agent.tools.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP (Model Context Protocol) 服务器
 * 提供标准化的工具调用接口
 */
@Component
public class MCPServer {

    private static final Logger logger = LoggerFactory.getLogger(MCPServer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, AgentTool> tools;

    public MCPServer(List<AgentTool> agentTools) {
        this.tools = agentTools.stream()
                .collect(Collectors.toMap(
                        tool -> tool.getDefinition().getName(),
                        tool -> tool
                ));
    }

    /**
     * 处理MCP请求
     */
    public MCPResponse handleRequest(MCPRequest request) {
        try {
            switch (request.getMethod()) {
                case "initialize":
                    return handleInitialize(request);
                case "tools/list":
                    return handleToolsList(request);
                case "tools/call":
                    return handleToolsCall(request);
                default:
                    return createErrorResponse(request.getId(), -32601,
                            "Method not found: " + request.getMethod());
            }
        } catch (Exception e) {
            logger.error("处理MCP请求失败", e);
            return createErrorResponse(request.getId(), -32603,
                    "Internal error: " + e.getMessage());
        }
    }

    private MCPResponse handleInitialize(MCPRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Map.of(
                "tools", Map.of("listChanged", true)
        ));
        result.put("serverInfo", Map.of(
                "name", "ai-platform-mcp-server",
                "version", "1.0.0"
        ));

        return createSuccessResponse(request.getId(), result);
    }

    private MCPResponse handleToolsList(MCPRequest request) {
        List<Map<String, Object>> toolsList = tools.values().stream()
                .map(tool -> {
                    Map<String, Object> toolMap = new HashMap<>();
                    toolMap.put("name", tool.getDefinition().getName());
                    toolMap.put("description", tool.getDefinition().getDescription());
                    toolMap.put("inputSchema", tool.getDefinition().getParameters());
                    return toolMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolsList);

        return createSuccessResponse(request.getId(), result);
    }

    private MCPResponse handleToolsCall(MCPRequest request) {
        try {
            Map<String, Object> params = request.getParams();
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

            AgentTool tool = tools.get(toolName);
            if (tool == null) {
                return createErrorResponse(request.getId(), -32602,
                        "Tool not found: " + toolName);
            }

            var toolResult = tool.execute(arguments);

            Map<String, Object> result = new HashMap<>();
            result.put("content", List.of(Map.of(
                    "type", "text",
                    "text", toolResult.getResult()
            )));
            result.put("isError", !toolResult.isSuccess());

            return createSuccessResponse(request.getId(), result);
        } catch (Exception e) {
            logger.error("工具调用失败", e);
            return createErrorResponse(request.getId(), -32603,
                    "Tool execution failed: " + e.getMessage());
        }
    }

    private MCPResponse createSuccessResponse(String id, Object result) {
        MCPResponse response = new MCPResponse();
        response.setId(id);
        response.setResult(result);
        return response;
    }

    private MCPResponse createErrorResponse(String id, int code, String message) {
        MCPResponse response = new MCPResponse();
        response.setId(id);
        response.setError(new MCPError(code, message));
        return response;
    }
}