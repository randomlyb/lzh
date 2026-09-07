package com.travlejava.ai.service.rag;

import com.travlejava.ai.pojo.rag.dto.ChatRequest;
import com.travlejava.ai.pojo.rag.dto.ChatResponse;
import com.travlejava.ai.service.ChatHistoryService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private final RAGService ragService;
    private final ChatHistoryService chatHistoryService;
    private final StringRedisTemplate redisTemplate;

    public RAGController(RAGService ragService, ChatHistoryService chatHistoryService, StringRedisTemplate redisTemplate) {
        this.ragService = ragService;
        this.chatHistoryService = chatHistoryService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/chat")
    public Mono<ResponseEntity<ChatResponse>> chat(@RequestBody ChatRequest chatRequest) {
        System.out.println("========== RAGController.chat 被调用 ==========");
        System.out.println("question: " + chatRequest.getQuestion());
        System.out.println("userId: " + chatRequest.getUserId());
        System.out.println("conversationId: " + chatRequest.getConversationId());
        
        return ragService.chat(chatRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    System.out.println("========== RAGController.chat 发生异常 ==========");
                    e.printStackTrace();
                    ChatResponse fallback = new ChatResponse(
                            "服务异常：" + e.getMessage(),
                            List.of(),
                            null,
                            chatRequest.getConversationId()
                    );
                    return Mono.just(ResponseEntity.status(500).body(fallback));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam String userId,
            @RequestParam String conversationId) {
        try {
            List<Map<String, Object>> history = chatHistoryService.getHistory(userId, conversationId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<String>> getConversations(@RequestParam String userId) {
        try {
            List<String> conversations = chatHistoryService.getConversationList(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/debug/keys")
    public ResponseEntity<Map<String, Object>> debugKeys() {
        try {
            // 获取所有 key
            Set<String> allKeys = redisTemplate.keys("*");
            
            // 获取所有 chat 相关的 key
            Set<String> chatKeys = redisTemplate.keys("chat:*");
            
            // 获取所有 history 相关的 key
            Set<String> historyKeys = redisTemplate.keys("*history*");
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("totalKeys", allKeys != null ? allKeys.size() : 0);
            result.put("chatKeys", chatKeys);
            result.put("historyKeys", historyKeys);
            result.put("allKeys", allKeys);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(
            @RequestParam String userId,
            @RequestParam String conversationId) {
        try {
            chatHistoryService.clearHistory(userId, conversationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}