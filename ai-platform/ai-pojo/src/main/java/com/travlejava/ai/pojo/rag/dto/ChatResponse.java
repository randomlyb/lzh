package com.travlejava.ai.pojo.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String answer;
    private List<String> sourceIds; // IDs of document chunks used for retrieval
    private Integer tokenUsage; // Number of tokens used in the conversation
    private String conversationId; // ID of the conversation
}
