package com.travlejava.ai.pojo.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_document_metadata")
public class DocumentMetadata {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String filePath;
    private String fileName;
    private String fileType;
    private String status;
    private LocalDateTime uploadTime;
    private LocalDateTime lastUpdateTime;
    private Long knowledgeBaseId;
}