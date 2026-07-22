package com.travlejava.ai.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travlejava.ai.pojo.document.dto.DocumentResponse;
import com.travlejava.ai.pojo.entity.document.DocumentMetadata;
import com.travlejava.ai.service.mapper.DocumentMapper;
import com.travlejava.ai.service.mq.DocumentEventProducer;
import com.travlejava.taskscheduler.common.mq.DocumentParseMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentService extends ServiceImpl<DocumentMapper, DocumentMetadata> {

    private final DocumentEventProducer documentEventProducer;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public DocumentService(DocumentEventProducer documentEventProducer) {
        this.documentEventProducer = documentEventProducer;
    }

    public DocumentResponse uploadDocument(MultipartFile file, String title, String description, Long knowledgeBaseId) throws IOException {
        // 1. Save the file to local storage (or object storage like S3)
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // 2. Save document metadata to database
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setTitle(title);
        metadata.setDescription(description);
        metadata.setFilePath(filePath.toString());
        metadata.setFileName(file.getOriginalFilename());
        metadata.setFileType(file.getContentType());
        metadata.setStatus("UPLOADED"); // Initial status
        metadata.setUploadTime(LocalDateTime.now());
        metadata.setLastUpdateTime(LocalDateTime.now());
        metadata.setKnowledgeBaseId(knowledgeBaseId);
        save(metadata);

        // 3. Send message to RabbitMQ to trigger parsing in scheduler-service
        DocumentParseMessage message = new DocumentParseMessage(
                metadata.getId(),
                metadata.getFilePath(),
                metadata.getFileType()
        );
        documentEventProducer.sendDocumentParseMessage(message);

        return DocumentResponse.fromDocumentMetadata(metadata);
    }

    public DocumentResponse getDocumentById(Long id) {
        DocumentMetadata metadata = getById(id);
        if (metadata == null) {
            return null;
        }
        return DocumentResponse.fromDocumentMetadata(metadata);
    }

    public void updateDocumentStatus(Long documentId, String status, String message) {
        DocumentMetadata metadata = getById(documentId);
        if (metadata != null) {
            metadata.setStatus(status);
            metadata.setLastUpdateTime(LocalDateTime.now());
            // Optionally store error message if status is FAILED
            if ("FAILED".equals(status)) {
                // metadata.setErrorMessage(message);
            }
            updateById(metadata);
        }
    }

    public void deleteDocument(Long id) throws IOException {
        DocumentMetadata metadata = getById(id);
        if (metadata != null) {
            // Delete file from storage
            Files.deleteIfExists(Paths.get(metadata.getFilePath()));
            removeById(id);
        }
    }
}
