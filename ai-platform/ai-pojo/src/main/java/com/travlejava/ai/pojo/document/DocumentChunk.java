
package com.travlejava.ai.pojo.document;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文档分片实体 * RAG (Retrieval-Augmented Generation) 系统的核心表
 * 存储文档的分片内容，用于向量化存储和检  */
@Data
@TableName("ai_document_chunk")
public class DocumentChunk {
    
    /**
     * 主键ID     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联的文档ID，指ai_knowledge_document     */
    private Long documentId;

    
    /**
     * 分块序号，标识该分片在文档中的位置顺     */
    private Integer chunkNo;
    
    /**
     * 分块内容，存储该片段的文本内     */
    private String content;
    
    /**
     * Token数量，统计该分块的Token数，用于计费和控制上下文长度
     */
    private Integer tokenCount;
    
    /**
     * 向量化状态：0-待处理，1-处理中，2-成功，3-失败
     */
    private Integer embeddingStatus;
}
