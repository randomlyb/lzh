package com.travlejava.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ChatHistoryService {

    private static final String CHAT_HISTORY_KEY_PREFIX = "chat:history:";
    private static final String CONVERSATION_LIST_KEY_PREFIX = "chat:conversations:";
    private static final long EXPIRE_DAYS = 7;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatHistoryService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveMessage(String userId, String conversationId, String role, String content, List<String> sourceIds) {
        try {
            String userIdStr = userId != null ? userId : "anonymous";
            String key = CHAT_HISTORY_KEY_PREFIX + userIdStr + ":" + conversationId;
            
            Map<String, Object> message = new java.util.HashMap<>();
            message.put("role", role);
            message.put("content", content);
            message.put("sourceIds", sourceIds != null ? sourceIds : new ArrayList<>());
            message.put("timestamp", System.currentTimeMillis());
            
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(key, messageJson);
            redisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);
            
            // 将 conversationId 添加到用户的对话列表中
            String conversationListKey = CONVERSATION_LIST_KEY_PREFIX + userIdStr;
            redisTemplate.opsForSet().add(conversationListKey, conversationId);
            redisTemplate.expire(conversationListKey, EXPIRE_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new RuntimeException("保存对话记录失败", e);
        }
    }

    public List<Map<String, Object>> getHistory(String userId, String conversationId) {
        try {
            String key = CHAT_HISTORY_KEY_PREFIX + (userId != null ? userId : "anonymous") + ":" + conversationId;
            List<String> messages = redisTemplate.opsForList().range(key, 0, -1);
            
            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (String msgJson : messages) {
                result.add(objectMapper.readValue(msgJson, new TypeReference<Map<String, Object>>() {}));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("获取对话记录失败", e);
        }
    }

    public List<String> getConversationList(String userId) {
        try {
            String userIdStr = userId != null ? userId : "anonymous";
            String conversationListKey = CONVERSATION_LIST_KEY_PREFIX + userIdStr;
            
            // 先尝试从对话列表集合中获取
            Set<String> conversations = redisTemplate.opsForSet().members(conversationListKey);
            
            // 如果集合为空，扫描所有历史key来重建列表
            if (conversations == null || conversations.isEmpty()) {
                // 扫描当前用户的所有对话key
                conversations = scanAllConversationKeys(userId);
                // 将扫描到的对话ID保存到集合中
                if (!conversations.isEmpty()) {
                    redisTemplate.opsForSet().add(conversationListKey, conversations.toArray(new String[0]));
                    redisTemplate.expire(conversationListKey, EXPIRE_DAYS, TimeUnit.DAYS);
                }
            }
            
            if (conversations == null || conversations.isEmpty()) {
                return new ArrayList<>();
            }
            // 按时间戳排序（conversationId 是时间戳）
            return conversations.stream()
                .filter(id -> {
                    try {
                        Long.parseLong(id);
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .sorted((a, b) -> Long.compare(Long.parseLong(b), Long.parseLong(a)))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("获取对话列表失败", e);
        }
    }

    /**
     * 扫描Redis中指定用户的对话历史key
     */
    private Set<String> scanAllConversationKeys(String userId) {
        Set<String> conversationIds = new java.util.HashSet<>();
        String userIdStr = userId != null ? userId : "anonymous";
        String pattern = CHAT_HISTORY_KEY_PREFIX + userIdStr + ":*";
        
        try {
            // 使用keys命令直接获取所有匹配的key
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    // 提取conversationId: chat:history:userId:conversationId
                    String[] parts = key.split(":");
                    if (parts.length >= 4) {
                        String convId = parts[parts.length - 1];
                        conversationIds.add(convId);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        return conversationIds;
    }

    public void clearHistory(String userId, String conversationId) {
        String userIdStr = userId != null ? userId : "anonymous";
        String key = CHAT_HISTORY_KEY_PREFIX + userIdStr + ":" + conversationId;
        redisTemplate.delete(key);
        
        // 从对话列表中移除
        String conversationListKey = CONVERSATION_LIST_KEY_PREFIX + userIdStr;
        redisTemplate.opsForSet().remove(conversationListKey, conversationId);
    }
}