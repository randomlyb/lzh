package com.travlejava.taskscheduler.service.executor;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.taskscheduler.common.entity.BlogArticle;
import com.travlejava.taskscheduler.common.executor.Executor;
import com.travlejava.taskscheduler.common.model.Result;
import com.travlejava.taskscheduler.common.model.TaskExecuteParam;
import com.travlejava.taskscheduler.service.mapper.BlogArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("blogToKnowledgeSyncJobExecutor")
public class BlogToKnowledgeSyncJobExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(BlogToKnowledgeSyncJobExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DOCUMENT_INDEX = "documents";

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    @Autowired
    private ElasticsearchClient esClient;

    @Override
    public Result<String> execute(TaskExecuteParam param) {
        logger.info("开始执行博客同步到知识库任务，参数: {}", param.getExecutorParams());
        try {
            JsonNode params = objectMapper.readTree(param.getExecutorParams());
            int limit = params.has("limit") ? params.get("limit").asInt() : 50;

            List<BlogArticle> articles = blogArticleMapper.selectList(
                    new LambdaQueryWrapper<BlogArticle>()
                            .orderByDesc(BlogArticle::getCollectedTime)
                            .last("LIMIT " + limit)
            );

            if (articles.isEmpty()) {
                return Result.success("没有需要同步的博客文章");
            }

            int successCount = 0;
            for (BlogArticle article : articles) {
                try {
                    indexToElasticsearch(article);
                    successCount++;
                    logger.info("文章 [{}] 同步到知识库成功", article.getTitle());
                } catch (Exception e) {
                    logger.error("文章 [{}] 同步到知识库失败: {}", article.getTitle(), e.getMessage());
                }
            }

            logger.info("博客同步到知识库完成，成功 {} 篇", successCount);
            return Result.success("博客同步到知识库完成，成功 " + successCount + " 篇");
        } catch (Exception e) {
            logger.error("博客同步到知识库失败", e);
            return Result.error("博客同步到知识库失败: " + e.getMessage());
        }
    }

    private void indexToElasticsearch(BlogArticle article) throws Exception {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", String.valueOf(article.getId()));
        doc.put("title", article.getTitle());
        doc.put("content", article.getSummary() != null ? article.getSummary() : article.getTitle());
        doc.put("url", article.getSourceUrl());
        doc.put("tags", article.getTags() != null ? article.getTags() : "");
        doc.put("type", "blog");
        doc.put("collectedTime", article.getCollectedTime().toString());

        esClient.index(i -> i
                .index(DOCUMENT_INDEX)
                .id(String.valueOf(article.getId()))
                .document(doc)
        );
    }
}