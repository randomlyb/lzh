package com.travlejava.ai.pojo.document.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@Data
public class DocumentUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private String description;
    private MultipartFile file; // The actual file to upload
    private Long knowledgeBaseId; // Optional: to associate with a knowledge base
}
