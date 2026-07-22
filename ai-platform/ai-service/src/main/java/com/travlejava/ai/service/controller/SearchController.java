package com.travlejava.ai.service.controller;

import com.travlejava.ai.pojo.search.dto.SearchRequest;
import com.travlejava.ai.pojo.search.dto.SearchResponse;
import com.travlejava.ai.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) throws IOException {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }
}
