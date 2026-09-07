package com.travlejava.ai.pojo.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String type;
    private FunctionCall function;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCall implements Serializable {
        private String name;
        private String arguments;
    }
}