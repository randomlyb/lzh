package com.travlejava.ai.pojo.rag.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String question;
    private Long userId; // Assuming user context is needed
    private String conversationId; // Optional, for continuing conversations
}
