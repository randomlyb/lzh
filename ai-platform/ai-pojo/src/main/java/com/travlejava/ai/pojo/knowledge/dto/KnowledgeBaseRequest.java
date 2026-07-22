package com.travlejava.ai.pojo.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeBaseRequest {
    private String name;
    private String description;
    private List<String> tags;
}
