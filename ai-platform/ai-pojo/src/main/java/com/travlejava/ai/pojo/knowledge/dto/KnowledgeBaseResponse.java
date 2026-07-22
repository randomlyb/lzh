package com.travlejava.ai.pojo.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeBaseResponse {
    private Long id;
    private String name;
    private String description;
    private List<String> tags;
    private Integer documentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
