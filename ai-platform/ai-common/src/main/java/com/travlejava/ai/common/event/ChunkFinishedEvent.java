package com.travlejava.ai.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文档分片完成事件 * 当文档分片完成后，会发送此事件触发向量化流*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkFinishedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 文档ID，关联到 ai_knowledge_document      */
    private Long documentId;
    
    /**
     * 知识库ID，标识文档所属的知识     */
    private Long knowledgeBaseId;
}
