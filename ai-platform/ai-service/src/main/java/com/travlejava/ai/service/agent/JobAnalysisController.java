package com.travlejava.ai.service.agent;

import com.travlejava.ai.pojo.rag.dto.ChatRequest;
import com.travlejava.ai.pojo.rag.dto.ChatResponse;
import com.travlejava.ai.service.ChatHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/job")
public class JobAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(JobAnalysisController.class);
    private final JobAnalysisAgent jobAnalysisAgent;
    private final ChatHistoryService chatHistoryService;

    public JobAnalysisController(JobAnalysisAgent jobAnalysisAgent,
                                 ChatHistoryService chatHistoryService) {
        this.jobAnalysisAgent = jobAnalysisAgent;
        this.chatHistoryService = chatHistoryService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ChatResponse> analyze(@RequestBody ChatRequest chatRequest) {
        logger.info("收到任务分析请求: question={}, conversationId={}", 
                chatRequest.getQuestion(), chatRequest.getConversationId());
        
        try {
            ChatResponse chatResponse = jobAnalysisAgent.analyze(chatRequest).block();
            if (chatResponse != null) {
                logger.info("任务分析完成: answer={}", chatResponse.getAnswer());
                return ResponseEntity.ok(chatResponse);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("任务分析异常", e);
            ChatResponse fallback = new ChatResponse(
                    "分析服务异常：" + e.getMessage(),
                    java.util.List.of(),
                    null,
                    chatRequest.getConversationId()
            );
            return ResponseEntity.ok(fallback);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam String userId,
            @RequestParam String conversationId) {
        try {
            List<Map<String, Object>> history = chatHistoryService.getHistory(userId, conversationId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("获取任务分析历史失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}