package com.travlejava.ai.pojo.llm.deepseek;

import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekChatRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Message> messages;
    private String model;
    private Double temperature;
    private Integer max_tokens;
    private Boolean stream;
    private List<ToolDefinition> tools;
    private String tool_choice;

    public DeepSeekChatRequest(List<Message> messages, String model, Double temperature, Integer max_tokens, Boolean stream) {
        this.messages = messages;
        this.model = model;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.stream = stream;
        this.tools = null;
        this.tool_choice = null;
    }
}