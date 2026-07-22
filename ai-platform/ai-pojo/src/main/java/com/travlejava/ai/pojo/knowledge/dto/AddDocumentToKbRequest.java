package com.travlejava.ai.pojo.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class AddDocumentToKbRequest {
    private Long knowledgeBaseId;
    private List<Long> documentIds;
}
