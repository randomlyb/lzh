package com.travlejava.ai.pojo.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCPResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String jsonrpc = "2.0";
    private String id;
    private Object result;
    private MCPError error;
}