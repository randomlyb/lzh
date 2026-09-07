package com.travlejava.ai.pojo.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCPError implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int code;
    private String message;
}