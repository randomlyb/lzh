package com.travlejava.ai.pojo.document.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DocumentUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private String description;
    private String fileName;
    private Long knowledgeBaseId;
}