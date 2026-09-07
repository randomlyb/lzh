package com.travlejava.ai.pojo.mcp;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class MCPRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String jsonrpc = "2.0";
    private String id;
    private String method;
    private Map<String, Object> params;
}