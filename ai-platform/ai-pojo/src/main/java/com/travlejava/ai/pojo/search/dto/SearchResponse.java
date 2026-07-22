package com.travlejava.ai.pojo.search.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private long totalHits;
    private List<SearchResultItem> items;

    @Data
    public static class SearchResultItem {
        private String documentId;
        private String title;
        private String content;
        private String url;
        private double score;
    }
}
