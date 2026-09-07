package com.travlejava.ai.service.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标签提取工具
 */
@Component
public class TagExtractorTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(TagExtractorTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("extract_tags");
        definition.setDescription("从文章内容中提取关键词标签");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> textProp = new HashMap<>();
        textProp.put("type", "string");
        textProp.put("description", "要提取标签的文本");
        properties.put("text", textProp);
        
        Map<String, Object> countProp = new HashMap<>();
        countProp.put("type", "integer");
        countProp.put("description", "提取标签数量");
        properties.put("count", countProp);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("text"));
        
        definition.setParameters(parameters);
        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            String text = (String) arguments.get("text");
            Integer count = arguments.get("count") != null ? 
                    ((Number) arguments.get("count")).intValue() : 5;
            
            logger.info("执行标签提取工具: count={}", count);
            
            String mockResult = "[\"Spring Boot\", \"微服务\", \"Redis\", \"MySQL\", \"Docker\"]";
            
            return new ToolResult("tag_1", "extract_tags", mockResult, true);
        } catch (Exception e) {
            logger.error("标签提取工具执行失败", e);
            return new ToolResult("tag_1", "extract_tags", 
                    "提取失败：" + e.getMessage(), false);
        }
    }
}