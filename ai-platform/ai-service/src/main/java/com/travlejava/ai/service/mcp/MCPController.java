package com.travlejava.ai.service.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.mcp.MCPRequest;
import com.travlejava.ai.pojo.mcp.MCPResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mcp")
public class MCPController {

    private final MCPServer mcpServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MCPController(MCPServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    /**
     * MCP标准接口 - POST请求
     */
    @PostMapping
    public ResponseEntity<MCPResponse> handleMCPRequest(@RequestBody MCPRequest request) {
        MCPResponse response = mcpServer.handleRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * MCP标准接口 - SSE流式响应
     */
    @PostMapping(value = "/stream", produces = "text/event-stream")
    public ResponseEntity<String> handleMCPStreamRequest(@RequestBody MCPRequest request) {
        MCPResponse response = mcpServer.handleRequest(request);
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            String sseMessage = "data: " + jsonResponse + "\n\n";
            return ResponseEntity.ok(sseMessage);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}