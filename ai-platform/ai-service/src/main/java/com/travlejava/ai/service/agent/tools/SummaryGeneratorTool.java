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
 * 摘要生成工具
 */
@Component
public class SummaryGeneratorTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(SummaryGeneratorTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ToolDefinition getDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("generate_summary");
        definition.setDescription("根据文章标题和内容生成简洁摘要");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("type", "string");
        titleProp.put("description", "文章标题");
        properties.put("title", titleProp);
        
        Map<String, Object> contentProp = new HashMap<>();
        contentProp.put("type", "string");
        contentProp.put("description", "文章内容（可选）");
        properties.put("content", contentProp);
        
        Map<String, Object> maxLengthProp = new HashMap<>();
        maxLengthProp.put("type", "integer");
        maxLengthProp.put("description", "摘要最大长度");
        properties.put("max_length", maxLengthProp);
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("title"));
        
        definition.setParameters(parameters);
        return definition;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            String title = (String) arguments.get("title");
            String content = (String) arguments.get("content");
            Integer maxLength = arguments.get("max_length") != null ? 
                    ((Number) arguments.get("max_length")).intValue() : 100;
            
            logger.info("执行摘要生成工具: title={}, maxLength={}", title, maxLength);
            
            String mockResult = String.format(
                    "本文介绍了%s的核心概念和实践方法，" +
                    "涵盖了关键技术要点和最佳实践，" +
                    "为开发者提供了实用的参考指南。",
                    title
            );
            
            return new ToolResult("summary_1", "generate_summary", mockResult, true);
        } catch (Exception e) {
            logger.error("摘要生成工具执行失败", e);
            return new ToolResult("summary_1", "generate_summary", 
                    "生成失败：" + e.getMessage(), false);
        }
    }
}