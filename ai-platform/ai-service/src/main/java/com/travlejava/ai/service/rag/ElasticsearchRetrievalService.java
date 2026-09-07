package com.travlejava.ai.service.rag;

import com.travlejava.ai.pojo.document.DocumentChunk;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchRetrievalService {

    private static final String DOCUMENT_CHUNK_INDEX = "document_chunks";
    private static final String DOCUMENTS_INDEX = "documents";
    private final RestHighLevelClient esClient;
    private final ObjectMapper objectMapper;

    public ElasticsearchRetrievalService(RestHighLevelClient esClient, ObjectMapper objectMapper) {
        this.esClient = esClient;
        this.objectMapper = objectMapper;
    }

    public List<DocumentChunk> retrieveTopKChunks(String query, int topK) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String trimmedQuery = query.trim();
        List<DocumentChunk> allChunks = new ArrayList<>();

        // 1. 检索 document_chunks 索引（PDF文档切片）
        allChunks.addAll(searchIndex(DOCUMENT_CHUNK_INDEX, trimmedQuery, topK));

        // 2. 检索 documents 索引（博客文章）
        allChunks.addAll(searchIndex(DOCUMENTS_INDEX, trimmedQuery, topK));

        // 按相关性排序，取前 topK 个
        return allChunks.stream()
                .limit(topK)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<DocumentChunk> searchIndex(String indexName, String query, int topK) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.multiMatchQuery(query, "content", "title", "tags"));
        searchSourceBuilder.size(topK);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
        List<DocumentChunk> chunks = new ArrayList<>();

        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            DocumentChunk chunk = objectMapper.convertValue(sourceAsMap, DocumentChunk.class);
            chunks.add(chunk);
        }
        return chunks;
    }
}