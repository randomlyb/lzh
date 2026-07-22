package com.travlejava.ai.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travlejava.ai.pojo.knowledge.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
}
