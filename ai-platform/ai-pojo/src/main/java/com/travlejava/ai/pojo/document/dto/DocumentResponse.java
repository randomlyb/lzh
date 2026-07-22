package com.travlejava.ai.pojo.document.dto;

import com.travlejava.ai.pojo.entity.document.DocumentMetadata;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DocumentResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String title;
    private String description;
    private String filePath;
    private String fileType;
    private String status; // e.g., UPLOADED, PARSING, PARSED, FAILED
    private LocalDateTime uploadTime;
    private LocalDateTime lastUpdateTime;
    private Long knowledgeBaseId;

    public static DocumentResponse fromDocumentMetadata(DocumentMetadata metadata) {
        DocumentResponse response = new DocumentResponse();
        response.setId(metadata.getId());
        response.setTitle(metadata.getTitle());
        response.setDescription(metadata.getDescription());
        response.setFilePath(metadata.getFilePath());
        response.setFileType(metadata.getFileType());
        response.setStatus(metadata.getStatus());
        response.setUploadTime(metadata.getUploadTime());
        response.setLastUpdateTime(metadata.getLastUpdateTime());
        response.setKnowledgeBaseId(metadata.getKnowledgeBaseId());
        return response;
    }
}
