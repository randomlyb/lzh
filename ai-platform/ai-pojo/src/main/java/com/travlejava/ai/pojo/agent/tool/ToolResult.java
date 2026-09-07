package com.travlejava.ai.pojo.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String toolCallId;
    private String toolName;
    private String result;
    private boolean success;
}