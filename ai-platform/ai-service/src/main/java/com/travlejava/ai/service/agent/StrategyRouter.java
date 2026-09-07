package com.travlejava.ai.service.agent;

import org.springframework.stereotype.Component;

/**
 * 策略路由器
 * 根据文章分类选择合适的Prompt模板
 */
@Component
public class StrategyRouter {

    /**
     * 根据文章分类获取对应的Prompt模板
     */
    public String getPromptTemplate(String category) {
        return switch (category) {
            case "java" -> JAVA_PROMPT_TEMPLATE;
            case "ai" -> AI_PROMPT_TEMPLATE;
            case "database" -> DATABASE_PROMPT_TEMPLATE;
            case "frontend" -> FRONTEND_PROMPT_TEMPLATE;
            case "cloud" -> CLOUD_PROMPT_TEMPLATE;
            default -> GENERAL_PROMPT_TEMPLATE;
        };
    }

    private static final String JAVA_PROMPT_TEMPLATE = """
            你是一个专业的Java技术文章分析助手。请根据提供的文章标题完成以下任务：

            1. 生成一段简洁的技术摘要（100字以内），突出技术要点
            2. 提取3-5个Java相关的技术关键词标签

            请严格按照以下JSON格式返回结果：
            {
              "summary": "文章摘要内容",
              "tags": ["标签1", "标签2", "标签3"]
            }

            文章标题：%s

            请直接返回JSON，不要包含其他解释文字。
            """;

    private static final String AI_PROMPT_TEMPLATE = """
            你是一个专业的人工智能技术文章分析助手。请根据提供的文章标题完成以下任务：

            1. 生成一段简洁的技术摘要（100字以内），突出AI技术要点
            2. 提取3-5个AI相关的技术关键词标签

            请严格按照以下JSON格式返回结果：
            {
              "summary": "文章摘要内容",
              "tags": ["标签1", "标签2", "标签3"]
            }

            文章标题：%s

            请直接返回JSON，不要包含其他解释文字。
            """;

    private static final String DATABASE_PROMPT_TEMPLATE = """
            你是一个专业的数据库技术文章分析助手。请根据提供的文章标题完成以下任务：

            1. 生成一段简洁的技术摘要（100字以内），突出数据库技术要点
            2. 提取3-5个数据库相关的技术关键词标签

            请严格按照以下JSON格式返回结果：
            {
              "summary": "文章摘要内容",
              "tags": ["标签1", "标签2", "标签3"]
            }

            文章标题：%s

            请直接返回JSON，不要包含其他解释文字。
            """;

    private static final String FRONTEND_PROMPT_TEMPLATE = """
            你是一个专业的前端技术文章分析助手。请根据提供的文章标题完成以下任务：

            1. 生成一段简洁的技术摘要（100字以内），突出前端技术要点
            2. 提取3-5个前端相关的技术关键词标签

            请严格按照以下JSON格式返回结果：
            {
              "summary": "文章摘要内容",
              "tags": ["标签1", "标签2", "标签3"]
            }

            文章标题：%s

            请直接返回JSON，不要包含其他解释文字。
            """;

    private static final String CLOUD_PROMPT_TEMPLATE = """
            你是一个专业的云原生技术文章分析助手。请根据提供的文章标题完成以下任务：

            1. 生成一段简洁的技术摘要（100字以内），突出云原生技术要点
            2. 提取3-5个云原生相关的技术关键词标签

            请严格按照以下JSON格式返回结果：
            {
              "summary": "文章摘要内容",
              "tags": ["标签1", "标签2", "标签3"]
            }

            文章标题：%s

            请直接返回JSON，不要包含其他解释文字。
            """;

    private static final String GENERAL_PROMPT_TEMPLATE = """
            你是一个专业的技术文章分析助手。请根据提供的文章标题完成以下任务：

            1. 生成一段简洁的摘要（100字以内）
            2. 提取3-5个关键词标签

            请严格按照以下JSON格式返回结果：
            {
              "summary": "文章摘要内容",
              "tags": ["标签1", "标签2", "标签3"]
            }

            文章标题：%s

            请直接返回JSON，不要包含其他解释文字。
            """;
}