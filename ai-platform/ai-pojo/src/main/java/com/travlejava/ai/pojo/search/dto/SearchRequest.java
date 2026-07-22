package com.travlejava.ai.pojo.search.dto;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private int page = 1;
    private int size = 10;
}
