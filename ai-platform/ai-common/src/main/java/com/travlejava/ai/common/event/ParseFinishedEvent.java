package com.travlejava.ai.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文档解析完成事件* 当文档内容解析完成后，会发送此事件触发文档分片流程
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseFinishedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 文档ID，关联到 ai_knowledge_document 表
     */
    private Long documentId;

    /**
     * 知识库ID，标识文档所属的知识库
     */
    private Long knowledgeBaseId;
    
    /**
     * 分片数量，记录文档被分成了多少个片段
     */
    private Integer chunkCount;
}
