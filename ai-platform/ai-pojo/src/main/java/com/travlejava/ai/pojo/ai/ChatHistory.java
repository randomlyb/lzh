
package com.travlejava.ai.pojo.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天历史记录实体* 存储用户与AI的对话历史，包括问题、答案、引用的来源文档等信 */
@Data
@TableName("ai_chat_history")
public class ChatHistory {
    
    /**
     * 主键ID，     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID，标识发起对话的用户（支持数字ID和用户名）
     */
    private String userId;
    
    /**
     * 用户的问题文    */
    private String question;
    
    /**
     * AI生成的回答文    */
    private String answer;
    
    /**
     * 引用来源ID列表，存储本次回答引用的文档分片ID，多个ID用逗号分隔
     */
    private String sourceIds;
    
    /**
     * Token使用量，统计本次对话消耗的Token数量
     */
    private Integer tokenUsage;
    
    /**
     * 创建时间，记录本次对话的时间
     */
    private LocalDateTime createTime;
}