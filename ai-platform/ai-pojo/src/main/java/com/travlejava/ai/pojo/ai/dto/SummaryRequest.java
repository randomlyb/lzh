package com.travlejava.ai.pojo.ai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SummaryRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 文章标题
     */
    private String title;
    
    /**
     * 文章内容（可选）
     */
    private String content;
}