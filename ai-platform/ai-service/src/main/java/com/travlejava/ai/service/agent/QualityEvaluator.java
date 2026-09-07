package com.travlejava.ai.service.agent;

import com.travlejava.ai.pojo.agent.dto.AgentSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 结果评估器
 * 评估生成的摘要和标签质量
 */
@Component
public class QualityEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(QualityEvaluator.class);

    /**
     * 评估摘要质量
     * @param response 生成的摘要响应
     * @return 质量分数（0-100）
     */
    public int evaluate(AgentSummaryResponse response) {
        int score = 0;

        // 1. 摘要长度评估（30-150字为佳）
        if (response.getSummary() != null && !response.getSummary().trim().isEmpty()) {
            int length = response.getSummary().trim().length();
            if (length >= 30 && length <= 150) {
                score += 30;
            } else if (length >= 20 && length <= 200) {
                score += 20;
            } else if (length > 0) {
                score += 10;
            }
        }

        // 2. 标签数量评估（3-5个为佳）
        if (response.getTags() != null && !response.getTags().isEmpty()) {
            int tagCount = response.getTags().size();
            if (tagCount >= 3 && tagCount <= 5) {
                score += 30;
            } else if (tagCount >= 2 && tagCount <= 7) {
                score += 20;
            } else if (tagCount > 0) {
                score += 10;
            }
        }

        // 3. 标签质量评估（不包含空字符串或"技术"这样的通用词）
        if (response.getTags() != null) {
            long validTags = response.getTags().stream()
                    .filter(tag -> tag != null && !tag.trim().isEmpty() && !tag.equals("技术"))
                    .count();
            if (validTags == response.getTags().size()) {
                score += 20;
            } else if (validTags > 0) {
                score += 10;
            }
        }

        // 4. 分类准确性评估
        if (response.getCategory() != null && !response.getCategory().equals("general")) {
            score += 20;
        } else {
            score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * 判断是否需要重试
     * @param score 质量分数
     * @return true表示需要重试
     */
    public boolean needsRetry(int score) {
        return score < 60;
    }
}