
package com.travlejava.ai.pojo.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识文档实体 * 存储知识库中文档的基本信息和元数据，每个文档属于一个知识库
 */
@Data
@TableName("ai_knowledge_document")
public class KnowledgeDocument {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private Long documentId;

    private String title;

    private String sourceType;

    private String sourceUrl;

    private String contentHash;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}