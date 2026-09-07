package com.travlejava.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.search.dto.SearchRequest;
import com.travlejava.ai.pojo.search.dto.SearchResponse;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private final RestHighLevelClient esClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String DOCUMENT_INDEX = "documents";
    private static final String SEARCH_CACHE_PREFIX = "search:cache:";
    private static final long CACHE_EXPIRE_MINUTES = 10;

    public SearchService(RestHighLevelClient esClient, StringRedisTemplate redisTemplate) {
        this.esClient = esClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public SearchResponse search(SearchRequest searchRequest) {
        SearchResponse response = new SearchResponse();
        response.setTotalHits(0L);
        response.setItems(new ArrayList<>());

        if (searchRequest.getQuery() == null || searchRequest.getQuery().trim().isEmpty()) {
            return response;
        }

        String query = searchRequest.getQuery().trim();
        String cacheKey = SEARCH_CACHE_PREFIX + query + ":" + searchRequest.getPage() + ":" + searchRequest.getSize();

        try {
            // 1. 先查Redis缓存
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                logger.info("命中搜索缓存: {}", query);
                return objectMapper.readValue(cachedJson, SearchResponse.class);
            }

            // 2. 缓存未命中,查询ES
            logger.info("未命中缓存,查询ES: {}", query);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.multiMatchQuery(query, "title", "content", "tags"));
            sourceBuilder.from((searchRequest.getPage() - 1) * searchRequest.getSize());
            sourceBuilder.size(searchRequest.getSize());

            org.elasticsearch.action.search.SearchRequest esSearchRequest = new org.elasticsearch.action.search.SearchRequest(DOCUMENT_INDEX);
            esSearchRequest.source(sourceBuilder);

            org.elasticsearch.action.search.SearchResponse esSearchResponse = esClient.search(esSearchRequest, RequestOptions.DEFAULT);

            response.setTotalHits(esSearchResponse.getHits().getTotalHits().value);

            List<SearchResponse.SearchResultItem> items = new ArrayList<>();
            for (SearchHit hit : esSearchResponse.getHits().getHits()) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                SearchResponse.SearchResultItem item = new SearchResponse.SearchResultItem();
                item.setDocumentId((String) sourceAsMap.get("id"));
                item.setTitle((String) sourceAsMap.get("title"));
                item.setContent((String) sourceAsMap.get("content"));
                item.setUrl((String) sourceAsMap.get("url"));
                item.setScore(hit.getScore());
                items.add(item);
            }
            response.setItems(items);

            // 3. 将结果存入缓存
            if (!items.isEmpty()) {
                String jsonResult = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(cacheKey, jsonResult, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                logger.info("搜索结果已缓存: {}, 结果数: {}", query, items.size());
            }

        } catch (ElasticsearchException e) {
            logger.error("Elasticsearch search failed: {}", e.getMessage());
        } catch (IOException e) {
            logger.error("Search IO error: {}", e.getMessage());
        }

        return response;
    }
}