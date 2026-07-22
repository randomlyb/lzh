
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
    
    /**
     * 主键ID，自增     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属知识库ID，关联到 ai_knowledge_base     */
    private Long knowledgeBaseId;
    
    /**
     * 文档标题，不能为空     */
    private String title;
    
    /**
     * 来源类型：GITHUB/RSS/PDF/WORD/MARKDOWN      */
    private String sourceType;
    
    /**
     * 来源URL，文档的原始地址
     */
    private String sourceUrl;
    
    /**
     * 内容Hash值，用于检测文档内容是否发生化?     */
    private String contentHash;
    
    /**
     * 文档状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 创建时间，记录文档何时添加到知识库     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间，记录文档最后更新时间     */
    private LocalDateTime updateTime;
}
