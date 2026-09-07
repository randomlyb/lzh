package com.travlejava.ai.pojo.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 生成的摘要
     */
    private String summary;
    
    /**
     * 提取的标签列表
     */
    private List<String> tags;
    
    /**
     * 文章分类
     */
    private String category;
    
    /**
     * 使用的策略
     */
    private String strategy;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 评估分数（0-100）
     */
    private Integer qualityScore;
}