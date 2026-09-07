package com.travlejava.ai.pojo.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 生成的摘要
     */
    private String summary;
    
    /**
     * 提取的标签列表
     */
    private List<String> tags;
}