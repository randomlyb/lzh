package com.travlejava.ai.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文档上传事件 * 当文档上传到知识库后，会发送此事件触发文档解析流程
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 文档ID，关联到 ai_knowledge_document     */
    private Long documentId;
    
    /**
     * 知识库ID，标识文档所属的知识     */
    private Long knowledgeBaseId;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 文件类型，如 PDF、DOCX、MD      */
    private String fileType;
    
    /**
     * 文件存储路径
     */
    private String filePath;
}
