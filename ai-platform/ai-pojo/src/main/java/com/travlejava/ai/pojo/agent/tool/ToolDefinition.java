package com.travlejava.ai.pojo.agent.tool;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ToolDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String description;
    private Map<String, Object> parameters;
}