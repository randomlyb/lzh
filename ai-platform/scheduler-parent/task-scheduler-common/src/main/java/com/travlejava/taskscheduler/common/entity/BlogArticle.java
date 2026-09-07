package com.travlejava.taskscheduler.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_blog_article")
public class BlogArticle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String summary;
    private String sourceUrl;
    private String tags;
    private LocalDateTime collectedTime;
}