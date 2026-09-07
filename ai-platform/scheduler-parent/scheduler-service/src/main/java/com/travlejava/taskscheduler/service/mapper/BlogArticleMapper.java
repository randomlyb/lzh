package com.travlejava.taskscheduler.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travlejava.taskscheduler.common.entity.BlogArticle;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {
}