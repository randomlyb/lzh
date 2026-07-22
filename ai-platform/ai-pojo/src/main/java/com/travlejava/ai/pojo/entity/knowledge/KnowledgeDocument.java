package com.travlejava.ai.pojo.entity.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge_document")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeBaseId;
    private String title;
    private String sourceType;
    private String sourceUrl;
    private String contentHash;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
