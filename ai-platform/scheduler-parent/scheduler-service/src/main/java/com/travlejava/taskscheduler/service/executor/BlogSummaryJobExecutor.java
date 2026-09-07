package com.travlejava.taskscheduler.service.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.taskscheduler.common.entity.BlogArticle;
import com.travlejava.taskscheduler.common.executor.Executor;
import com.travlejava.taskscheduler.common.model.Result;
import com.travlejava.taskscheduler.common.model.TaskExecuteParam;
import com.travlejava.taskscheduler.service.mapper.BlogArticleMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component("blogSummaryJobExecutor")
public class BlogSummaryJobExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(BlogSummaryJobExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String LOCK_KEY = "lock:blog_summary";
    private static final long LOCK_WAIT_TIME = 10;
    private static final long LOCK_LEASE_TIME = 300;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    @Autowired
    @Qualifier("externalRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Result<String> execute(TaskExecuteParam param) {
        logger.info("开始执行AI摘要生成任务，参数: {}", param.getExecutorParams());
        
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            // 尝试获取分布式锁,等待10秒,锁自动释放时间300秒
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                logger.info("成功获取分布式锁,开始执行摘要生成任务");
                return doExecute(param);
            } else {
                logger.warn("获取分布式锁失败,任务正在其他实例执行");
                return Result.error("任务正在执行中,请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取分布式锁被中断", e);
            return Result.error("任务执行被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.info("释放分布式锁");
            }
        }
    }

    private Result<String> doExecute(TaskExecuteParam param) {
        try {
            JsonNode params = objectMapper.readTree(param.getExecutorParams());
            Long articleId = params.has("articleId") ? params.get("articleId").asLong() : null;
            int limit = params.has("limit") ? params.get("limit").asInt() : 10;

            List<BlogArticle> articles;
            if (articleId != null) {
                BlogArticle article = blogArticleMapper.selectById(articleId);
                articles = article != null ? List.of(article) : List.of();
            } else {
                articles = blogArticleMapper.selectList(
                        new LambdaQueryWrapper<BlogArticle>()
                                .and(w -> w.isNull(BlogArticle::getTags).or().eq(BlogArticle::getTags, "").or().like(BlogArticle::getSummary, "服务异常"))
                                .orderByAsc(BlogArticle::getId)
                                .last("LIMIT " + limit)
                );
            }

            if (articles.isEmpty()) {
                return Result.success("没有需要生成摘要的文章");
            }

            int successCount = 0;
            for (BlogArticle article : articles) {
                try {
                    String[] result = generateSummaryAndTags(article);
                    article.setSummary(result[0]);
                    article.setTags(result[1]);
                    blogArticleMapper.updateById(article);
                    successCount++;
                    logger.info("文章 [{}] 摘要生成成功", article.getTitle());
                } catch (Exception e) {
                    logger.error("文章 [{}] 摘要生成失败: {}", article.getTitle(), e.getMessage());
                }
            }

            logger.info("AI摘要生成完成，成功 {} 篇", successCount);
            return Result.success("AI摘要生成完成，成功 " + successCount + " 篇");
        } catch (Exception e) {
            logger.error("AI摘要生成失败", e);
            return Result.error("AI摘要生成失败: " + e.getMessage());
        }
    }

    private String[] generateSummaryAndTags(BlogArticle article) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "title", article.getTitle(),
                "maxRetries", 2
        );

        HttpEntity<String> entity = new HttpEntity<>(toJson(body), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:8083/api/agent/summary",
                entity,
                String.class
        );

        String[] result = new String[2];
        result[0] = article.getTitle();
        result[1] = "技术";

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                JsonNode node = objectMapper.readTree(response.getBody());
                if (node.has("summary")) {
                    String summary = node.get("summary").asText();
                    if (summary != null && !summary.trim().isEmpty() && !summary.contains("服务异常")) {
                        result[0] = summary;
                    }
                }
                if (node.has("tags") && node.get("tags").isArray()) {
                    List<String> tags = new ArrayList<>();
                    node.get("tags").forEach(tag -> tags.add(tag.asText()));
                    if (!tags.isEmpty()) {
                        result[1] = String.join(",", tags);
                    }
                }
            } catch (Exception e) {
                logger.warn("解析摘要响应失败", e);
            }
        }

        return result;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}