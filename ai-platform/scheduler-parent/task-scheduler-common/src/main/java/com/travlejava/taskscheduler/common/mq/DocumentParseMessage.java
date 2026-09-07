package com.travlejava.taskscheduler.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long documentId;
    private String documentPath;
    private String fileType;
    private String title;
    private String description;
    private Long knowledgeBaseId;
    private LocalDateTime uploadTime;
}