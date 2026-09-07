package com.travlejava.ai.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travlejava.ai.service.mapper.BlogArticleMapper;
import com.travlejava.taskscheduler.common.entity.BlogArticle;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogArticleMapper blogArticleMapper;

    public BlogController(BlogArticleMapper blogArticleMapper) {
        this.blogArticleMapper = blogArticleMapper;
    }

    @GetMapping
    public ResponseEntity<List<BlogArticle>> getAllBlogs(
            @RequestParam(defaultValue = "100") Integer limit) {
        QueryWrapper<BlogArticle> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("collected_time");
        queryWrapper.last("LIMIT " + limit);
        List<BlogArticle> blogs = blogArticleMapper.selectList(queryWrapper);
        return ResponseEntity.ok(blogs);
    }
}