package com.travlejava.ai.pojo.llm.deepseek;

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
}
