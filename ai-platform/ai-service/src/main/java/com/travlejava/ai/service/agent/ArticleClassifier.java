package com.travlejava.ai.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 文章分类器
 * 根据标题和内容判断文章类型
 */
@Component
public class ArticleClassifier {

    private static final Logger logger = LoggerFactory.getLogger(ArticleClassifier.class);

    // Java相关关键词
    private static final Pattern JAVA_PATTERN = Pattern.compile(
            "(?i)(java|spring|jvm|mybatis|hibernate|tomcat|maven|gradle|jdk)");

    // AI相关关键词
    private static final Pattern AI_PATTERN = Pattern.compile(
            "(?i)(ai|人工智能|机器学习|深度学习|神经网络|大模型|llm|gpt|transformer|nlp|cv)");

    // 数据库相关关键词
    private static final Pattern DATABASE_PATTERN = Pattern.compile(
            "(?i)(mysql|redis|mongodb|elasticsearch|sql|nosql|数据库|索引|事务|缓存)");

    // 前端相关关键词
    private static final Pattern FRONTEND_PATTERN = Pattern.compile(
            "(?i)(vue|react|angular|javascript|typescript|html|css|webpack|nodejs)");

    // 云原生相关关键词
    private static final Pattern CLOUD_PATTERN = Pattern.compile(
            "(?i)(docker|kubernetes|k8s|微服务|kafka|rabbitmq|nginx|负载均衡|devops)");

    /**
     * 分类文章
     * @param title 文章标题
     * @param content 文章内容（可选）
     * @return 文章分类
     */
    public String classify(String title, String content) {
        String text = (title != null ? title : "") + " " + (content != null ? content : "");

        int javaScore = countMatches(text, JAVA_PATTERN);
        int aiScore = countMatches(text, AI_PATTERN);
        int dbScore = countMatches(text, DATABASE_PATTERN);
        int frontendScore = countMatches(text, FRONTEND_PATTERN);
        int cloudScore = countMatches(text, CLOUD_PATTERN);

        int maxScore = Math.max(Math.max(Math.max(javaScore, aiScore), dbScore),
                Math.max(frontendScore, cloudScore));

        if (maxScore == 0) {
            return "general";
        }

        if (javaScore == maxScore) return "java";
        if (aiScore == maxScore) return "ai";
        if (dbScore == maxScore) return "database";
        if (frontendScore == maxScore) return "frontend";
        if (cloudScore == maxScore) return "cloud";

        return "general";
    }

    private int countMatches(String text, Pattern pattern) {
        return (int) pattern.matcher(text).results().count();
    }
}