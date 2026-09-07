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
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("spiderCollectJobExecutor")
public class SpiderCollectJobExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(SpiderCollectJobExecutor.class);
    private static final String DEFAULT_URL = "https://www.cnblogs.com/";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DOCUMENT_INDEX = "documents";
    private static final String BLOOM_FILTER_KEY = "bloom:blog_urls";
    private static final long BLOOM_EXPECTED_INSERTIONS = 1000000;
    private static final double BLOOM_FALSE_POSITIVE_RATE = 0.01;

    @Autowired
    private BlogArticleMapper blogArticleMapper;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("externalRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private RedissonClient redissonClient;

    private RBloomFilter<String> bloomFilter;

    @Override
    public Result<String> execute(TaskExecuteParam param) {
        logger.info("开始执行博客采集任务，参数: {}", param.getExecutorParams());
        try {
            // 初始化布隆过滤器
            initBloomFilter();
            
            String targetUrl = DEFAULT_URL;
            if (param.getExecutorParams() != null && !param.getExecutorParams().isEmpty()) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(param.getExecutorParams());
                    if (jsonNode.has("url") && !jsonNode.get("url").asText().isEmpty()) {
                        targetUrl = jsonNode.get("url").asText();
                    }
                } catch (Exception e) {
                    logger.warn("解析执行参数失败，使用默认URL: {}", e.getMessage());
                }
            }

            int collectedCount = collectBlogs(targetUrl);
            logger.info("博客采集完成，共采集 {} 篇", collectedCount);
            return Result.success("博客采集完成，共采集 " + collectedCount + " 篇");
        } catch (Exception e) {
            logger.error("博客采集失败", e);
            return Result.error("博客采集失败: " + e.getMessage());
        }
    }

    /**
     * 初始化布隆过滤器
     */
    private void initBloomFilter() {
        try {
            bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_KEY);
            if (!bloomFilter.isExists()) {
                bloomFilter.tryInit(BLOOM_EXPECTED_INSERTIONS, BLOOM_FALSE_POSITIVE_RATE);
                logger.info("布隆过滤器初始化成功，预期容量: {}, 误判率: {}", 
                        BLOOM_EXPECTED_INSERTIONS, BLOOM_FALSE_POSITIVE_RATE);
            } else {
                logger.info("布隆过滤器已存在，直接使用");
            }
        } catch (Exception e) {
            logger.error("布隆过滤器初始化失败", e);
        }
    }

    private int collectBlogs(String targetUrl) throws Exception {
        logger.info("开始从博客园采集技术文章...");

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                new URI(targetUrl),
                HttpMethod.GET,
                entity,
                String.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("请求失败，状态码: " + response.getStatusCode());
        }

        String htmlContent = response.getBody();
        if (htmlContent == null || htmlContent.isEmpty()) {
            logger.warn("返回内容为空");
            return 0;
        }

        List<BlogArticle> articles = new ArrayList<>();
        int skippedByBloomFilter = 0;
        int skippedByDatabase = 0;

        // 匹配博客园首页文章卡片
        Pattern articlePattern = Pattern.compile(
            "<a\\s+class=\"post-item-title\"\\s+href=\"([^\"]+)\"[^>]*>([^<]+)</a>.*?" +
            "<p\\s+class=\"post-item-summary\">\\s*([^<]+)",
            Pattern.DOTALL
        );
        Matcher matcher = articlePattern.matcher(htmlContent);

        while (matcher.find()) {
            try {
                String link = matcher.group(1).trim();
                String title = matcher.group(2).trim();
                String summary = matcher.group(3).trim();

                if (title.isEmpty() || link.isEmpty()) {
                    continue;
                }

                // 第一层过滤:布隆过滤器快速检查
                if (bloomFilter != null && bloomFilter.contains(link)) {
                    skippedByBloomFilter++;
                    logger.debug("布隆过滤器拦截: {}", title);
                    continue;
                }

                // 第二层过滤:数据库精确检查
                long exists = blogArticleMapper.selectCount(
                        new LambdaQueryWrapper<BlogArticle>()
                                .eq(BlogArticle::getSourceUrl, link)
                );
                if (exists > 0) {
                    skippedByDatabase++;
                    // 添加到布隆过滤器
                    if (bloomFilter != null) {
                        bloomFilter.add(link);
                    }
                    continue;
                }

                // 添加到布隆过滤器
                if (bloomFilter != null) {
                    bloomFilter.add(link);
                }

                BlogArticle article = new BlogArticle();
                article.setTitle(title);
                article.setSummary(summary.length() > 500 ? summary.substring(0, 500) : summary);
                article.setSourceUrl(link);
                article.setCollectedTime(LocalDateTime.now());

                articles.add(article);
            } catch (Exception e) {
                logger.warn("解析文章项失败", e);
            }
        }

        logger.info("布隆过滤器拦截: {} 篇, 数据库拦截: {} 篇", skippedByBloomFilter, skippedByDatabase);

        if (!articles.isEmpty()) {
            for (BlogArticle article : articles) {
                try {
                    blogArticleMapper.insert(article);
                    logger.info("保存文章: {}", article.getTitle());

                    // 同步到 ES 知识库
                    indexToElasticsearch(article);

                    // 异步调用 AI 服务生成智能摘要
                    generateAiSummaryAsync(article);
                } catch (Exception e) {
                    logger.error("保存文章失败: {}", article.getTitle(), e);
                }
            }
            logger.info("成功保存 {} 篇新文章到数据库", articles.size());
        } else {
            logger.info("没有新文章需要保存");
        }

        return articles.size();
    }

    /**
     * 异步调用 AI 服务生成博客摘要
     */
    private void generateAiSummaryAsync(BlogArticle article) {
        try {
            // 获取文章完整内容
            String content = fetchArticleContent(article.getSourceUrl());
            if (content == null || content.isEmpty()) {
                logger.warn("无法获取文章内容，跳过 AI 摘要生成: {}", article.getTitle());
                return;
            }

            // 调用 ai-service 的摘要接口
            Map<String, String> request = new HashMap<>();
            request.put("title", article.getTitle());
            request.put("content", content);

            Map<String, Object> result = restTemplate.postForObject(
                    "http://localhost:8083/api/blog/summarize",
                    request,
                    Map.class
            );

            if (result != null) {
                String aiSummary = (String) result.get("summary");
                String aiTags = (String) result.get("tags");

                if (aiSummary != null && !aiSummary.isEmpty()) {
                    article.setSummary(aiSummary);
                    logger.info("AI 摘要生成成功: {}", article.getTitle());
                }

                if (aiTags != null && !aiTags.isEmpty()) {
                    article.setTags(aiTags);
                }

                // 更新数据库
                blogArticleMapper.updateById(article);
                logger.info("文章 [{}] 已更新 AI 摘要和标签", article.getTitle());
            }
        } catch (Exception e) {
            logger.error("AI 摘要生成失败: {}", article.getTitle(), e);
        }
    }

    /**
     * 获取文章完整内容
     */
    private String fetchArticleContent(String articleUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    new URI(articleUrl),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String html = response.getBody();
                // 提取文章正文内容
                return extractContentFromHtml(html);
            }
        } catch (Exception e) {
            logger.error("获取文章内容失败: {}", articleUrl, e);
        }
        return null;
    }

    /**
     * 从 HTML 中提取正文内容
     */
    private String extractContentFromHtml(String html) {
        if (html == null) return null;

        // 尝试匹配博客园文章正文
        Pattern contentPattern = Pattern.compile(
                "<div[^>]*id=\"post_detail\"[^>]*>.*?<div[^>]*class=\"post\"[^>]*>.*?" +
                "<div[^>]*class=\"postbody\"[^>]*>(.*?)</div>\\s*<div[^>]*class=\"postDesc\"",
                Pattern.DOTALL
        );
        Matcher matcher = contentPattern.matcher(html);
        if (matcher.find()) {
            String content = matcher.group(1);
            // 去除 HTML 标签
            return content.replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        // 备用方案：尝试匹配 cnblogs 文章内容
        Pattern altPattern = Pattern.compile(
                "<div[^>]*id=\"cnblogs_post_body\"[^>]*>(.*?)</div>",
                Pattern.DOTALL
        );
        Matcher altMatcher = altPattern.matcher(html);
        if (altMatcher.find()) {
            return altMatcher.group(1).replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        return null;
    }

    /**
     * 将博客文章索引到 Elasticsearch
     */
    private void indexToElasticsearch(BlogArticle article) {
        try {
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
            logger.info("文章 [{}] 已索引到 ES", article.getTitle());
        } catch (Exception e) {
            logger.error("索引文章到 ES 失败: {}", article.getTitle(), e);
        }
    }
}