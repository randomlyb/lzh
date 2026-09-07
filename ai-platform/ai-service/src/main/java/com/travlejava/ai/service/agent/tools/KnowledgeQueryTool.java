package com.travlejava.ai.service.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import com.travlejava.ai.pojo.knowledge.KnowledgeBase;
import com.travlejava.ai.pojo.knowledge.KnowledgeDocument;
import com.travlejava.ai.service.KnowledgeBaseService;
import com.travlejava.ai.service.mapper.KnowledgeDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库查询工具 - 真实MySQL查询
 */
@Component
public class KnowledgeQueryTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeQueryTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public KnowledgeQueryTool(KnowledgeBaseService knowledgeBaseService,
                              KnowledgeDocumentMapper knowledgeDocumentMapper) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
    }

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("query_knowledge");
        definition.setDescription("查询特定技术领域的专业知识库");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> domainProp = new HashMap<>();
        domainProp.put("type", "string");
        domainProp.put("description", "技术领域：java/ai/database/frontend/cloud");
        properties.put("domain", domainProp);
        
        Map<String, Object> topicProp = new HashMap<>();
        topicProp.put("type", "string");
        topicProp.put("description", "查询主题");
        properties.put("topic", topicProp);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("domain", "topic"));
        
        definition.setParameters(parameters);
        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            String domain = (String) arguments.get("domain");
            String topic = (String) arguments.get("topic");
            
            logger.info("执行知识库查询工具: domain={}, topic={}", domain, topic);
            
            // 1. 查找对应领域的知识库
            List<KnowledgeBase> knowledgeBases = knowledgeBaseService.list(
                    new LambdaQueryWrapper<KnowledgeBase>()
                            .like(KnowledgeBase::getName, domain)
                            .or()
                            .like(KnowledgeBase::getDescription, domain)
            );
            
            if (knowledgeBases.isEmpty()) {
                return new ToolResult("query_1", "query_knowledge", 
                        String.format("未找到【%s】领域的知识库", domain), true);
            }
            
            // 2. 查询知识库下的相关文档
            KnowledgeBase kb = knowledgeBases.get(0);
            List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeDocument>()
                            .eq(KnowledgeDocument::getKnowledgeBaseId, kb.getId())
                            .like(KnowledgeDocument::getTitle, topic)
            );
            
            if (documents.isEmpty()) {
                return new ToolResult("query_1", "query_knowledge", 
                        String.format("在【%s】知识库中未找到关于【%s】的文档", domain, topic), true);
            }
            
            // 3. 构建查询结果
            StringBuilder result = new StringBuilder();
            result.append(String.format("【%s领域】关于【%s】的知识库查询结果：\n\n", domain, topic));
            result.append(String.format("知识库: %s\n\n", kb.getName()));
            
            for (int i = 0; i < documents.size(); i++) {
                KnowledgeDocument doc = documents.get(i);
                result.append(String.format("%d. %s\n", i + 1, doc.getTitle()));
                result.append(String.format("   文档ID: %d\n", doc.getId()));
                result.append(String.format("   来源: %s\n\n", doc.getSourceUrl()));
            }
            
            return new ToolResult("query_1", "query_knowledge", result.toString(), true);
        } catch (Exception e) {
            logger.error("知识库查询工具执行失败", e);
            return new ToolResult("query_1", "query_knowledge", 
                    "查询失败：" + e.getMessage(), false);
        }
    }
}