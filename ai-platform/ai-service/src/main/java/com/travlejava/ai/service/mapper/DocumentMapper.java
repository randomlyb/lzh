package com.travlejava.ai.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travlejava.ai.pojo.document.entity.DocumentMetadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<DocumentMetadata> {
}