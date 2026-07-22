package com.travlejava.ai.service;

import com.travlejava.ai.pojo.search.dto.SearchRequest;
import com.travlejava.ai.pojo.search.dto.SearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final RestHighLevelClient esClient;
    private static final String DOCUMENT_INDEX = "documents"; // Assuming a 'documents' index in Elasticsearch

    public SearchService(RestHighLevelClient esClient) {
        this.esClient = esClient;
    }

    public SearchResponse search(SearchRequest searchRequest) throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("content", searchRequest.getQuery())); // Search in 'content' field
        sourceBuilder.from((searchRequest.getPage() - 1) * searchRequest.getSize());
        sourceBuilder.size(searchRequest.getSize());

        org.elasticsearch.action.search.SearchRequest esSearchRequest = new org.elasticsearch.action.search.SearchRequest(DOCUMENT_INDEX);
        esSearchRequest.source(sourceBuilder);

        org.elasticsearch.action.search.SearchResponse esSearchResponse = esClient.search(esSearchRequest, RequestOptions.DEFAULT);

        SearchResponse response = new SearchResponse();
        response.setTotalHits(esSearchResponse.getHits().getTotalHits().value);

        List<SearchResponse.SearchResultItem> items = new ArrayList<>();
        for (SearchHit hit : esSearchResponse.getHits().getHits()) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            SearchResponse.SearchResultItem item = new SearchResponse.SearchResultItem();
            item.setDocumentId((String) sourceAsMap.get("id")); // Assuming 'id' field in ES document
            item.setTitle((String) sourceAsMap.get("title")); // Assuming 'title' field in ES document
            item.setContent((String) sourceAsMap.get("content")); // Assuming 'content' field in ES document
            item.setUrl((String) sourceAsMap.get("url")); // Assuming 'url' field in ES document
            item.setScore(hit.getScore());
            items.add(item);
        }
        response.setItems(items);
        return response;
    }
}
