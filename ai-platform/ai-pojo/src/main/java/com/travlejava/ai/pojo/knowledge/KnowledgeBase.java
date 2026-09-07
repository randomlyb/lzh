
package com.travlejava.ai.pojo.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库实体类
 * 用于管理企业AI知识平台中的知识库信息，每个知识库可以包含多个文档
 */
@Data
@TableName("ai_knowledge_base")
public class KnowledgeBase {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private Long ownerId;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}