
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
    
    /**
     * 主键ID，自动生成
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 知识库名称，不能为空
     */
    private String name;
    
    /**
     * 知识库描述，说明知识库的用途和内容范围
     */
    private String description;
    
    /**
     * 所有者ID，标识创建该知识库的用户
     */
    private Long ownerId;
    
    /**
     * 知识库状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 创建时间，记录知识库何时创建
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间，记录知识库最后更新时间
     */
    private LocalDateTime updateTime;
}
