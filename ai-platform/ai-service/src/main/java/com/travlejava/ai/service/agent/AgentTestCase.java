package com.travlejava.ai.service.agent;

import lombok.Data;

import java.util.List;

/** Agent 评测用例 */
@Data
public class AgentTestCase {

    private Integer id;

    /** 用户问题 */
    private String question;

    /** 分类，用于分组统计 */
    private String category;

    /** 期望调用的工具名（召回率分母） */
    private List<String> expectedTools;

    /** 最终答案应包含的关键词 */
    private List<String> answerKeywords;

    /** 允许的最大迭代轮次 */
    private Integer maxSteps = 4;
}
