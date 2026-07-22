package com.travlejava.ai.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travlejava.ai.pojo.document.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
}
