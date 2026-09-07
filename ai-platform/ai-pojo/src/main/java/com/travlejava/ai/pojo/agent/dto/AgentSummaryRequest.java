package com.travlejava.ai.pojo.agent.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AgentSummaryRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 文章标题
     */
    private String title;
    
    /**
     * 文章内容（可选）
     */
    private String content;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetries = 2;
}