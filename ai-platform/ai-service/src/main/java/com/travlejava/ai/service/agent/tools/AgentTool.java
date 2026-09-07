package com.travlejava.ai.service.agent.tools;

import com.travlejava.ai.pojo.agent.tool.ToolDefinition;
import com.travlejava.ai.pojo.agent.tool.ToolResult;

import java.util.Map;

/**
 * Agent工具接口
 */
public interface AgentTool {
    
    /**
     * 获取工具定义
     */
    ToolDefinition getDefinition();
    
    /**
     * 执行工具
     * @param arguments 工具参数
     * @return 工具执行结果
     */
    ToolResult execute(Map<String, Object> arguments);
}