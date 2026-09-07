package com.travlejava.ai.pojo.llm.deepseek;

import com.travlejava.ai.pojo.agent.tool.ToolCall;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private String role;
    private String content;
    private List<ToolCall> tool_calls;
    private String tool_call_id;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
        this.tool_calls = null;
        this.tool_call_id = null;
    }
}