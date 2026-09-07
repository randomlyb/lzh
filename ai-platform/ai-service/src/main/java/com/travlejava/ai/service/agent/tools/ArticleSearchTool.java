package com.travlejava.ai.service.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import com.travlejava.ai.pojo.document.DocumentChunk;
import com.travlejava.ai.service.rag.ElasticsearchRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文章搜索工具 - 真实ES搜索
 */
@Component
public class ArticleSearchTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(ArticleSearchTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ElasticsearchRetrievalService retrievalService;

    public ArticleSearchTool(ElasticsearchRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("search_article");
        definition.setDescription("从知识库中搜索相关文章，获取相关上下文信息");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> queryProp = new HashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "搜索关键词或问题");
        properties.put("query", queryProp);
        
        Map<String, Object> limitProp = new HashMap<>();
        limitProp.put("type", "integer");
        limitProp.put("description", "返回结果数量限制");
        properties.put("limit", limitProp);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("query"));
        
        definition.setParameters(parameters);
        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            String query = (String) arguments.get("query");
            Integer limit = arguments.get("limit") != null ? 
                    ((Number) arguments.get("limit")).intValue() : 5;
            
            logger.info("执行文章搜索工具: query={}, limit={}", query, limit);
            
            // 真实ES搜索
            List<DocumentChunk> chunks = retrievalService.retrieveTopKChunks(query, limit);
            
            if (chunks.isEmpty()) {
                return new ToolResult("search_1", "search_article", 
                        "未找到相关文章", true);
            }
            
            // 构建搜索结果
            StringBuilder result = new StringBuilder();
            result.append(String.format("找到%d篇相关文章：\n\n", chunks.size()));
            
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                result.append(String.format("%d. [文档ID: %s, 分片: %s]\n", 
                        i + 1, chunk.getDocumentId(), chunk.getChunkNo()));
                result.append(String.format("   内容: %s\n\n", chunk.getContent()));
            }
            
            return new ToolResult("search_1", "search_article", result.toString(), true);
        } catch (Exception e) {
            logger.error("文章搜索工具执行失败", e);
            return new ToolResult("search_1", "search_article", 
                    "搜索失败：" + e.getMessage(), false);
        }
    }
}