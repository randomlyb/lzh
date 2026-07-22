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

    private static final String DOCUMENT_CHUNK_INDEX = "document_chunks"; // Assuming an index name
    private final RestHighLevelClient esClient;
    private final ObjectMapper objectMapper; // For converting ES hits to DocumentChunk objects

    public ElasticsearchRetrievalService(RestHighLevelClient esClient, ObjectMapper objectMapper) {
        this.esClient = esClient;
        this.objectMapper = objectMapper;
    }

    public List<DocumentChunk> retrieveTopKChunks(String query, int topK) throws IOException {
        SearchRequest searchRequest = new SearchRequest(DOCUMENT_CHUNK_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // Simple match query for now. In a real RAG, this would be a vector search or a more complex query.
        searchSourceBuilder.query(QueryBuilders.matchQuery("content", query));
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
