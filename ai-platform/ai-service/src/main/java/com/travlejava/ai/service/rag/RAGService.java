package com.travlejava.ai.service.rag;

import com.travlejava.ai.pojo.ai.ChatHistory;
import com.travlejava.ai.pojo.document.DocumentChunk;
import com.travlejava.ai.pojo.rag.dto.ChatRequest;
import com.travlejava.ai.pojo.rag.dto.ChatResponse;
import com.travlejava.ai.service.ChatHistoryService;
import com.travlejava.ai.service.mapper.ChatHistoryMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class RAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);

    private final ElasticsearchRetrievalService retrievalService;
    private final DeepSeekGenerationService generationService;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatHistoryService chatHistoryService;

    public RAGService(ElasticsearchRetrievalService retrievalService, 
                      DeepSeekGenerationService generationService, 
                      ChatHistoryMapper chatHistoryMapper,
                      ChatHistoryService chatHistoryService) {
        this.retrievalService = retrievalService;
        this.generationService = generationService;
        this.chatHistoryMapper = chatHistoryMapper;
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * RAG对话接口 - 带熔断器保护
     * 
     * 熔断器配置:
     * - 失败率>50%时打开熔断器
     * - 打开后10秒内拒绝所有请求,直接返回降级响应
     * - 10秒后进入半开状态,允许3个请求测试
     * - 如果测试成功,关闭熔断器;如果失败,重新打开
     */
    @CircuitBreaker(name = "ragService", fallbackMethod = "chatFallback")
    @TimeLimiter(name = "ragService")
    public CompletableFuture<ChatResponse> chatAsync(ChatRequest chatRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 检索相关文档片段
                List<DocumentChunk> retrievedChunks = retrievalService.retrieveTopKChunks(chatRequest.getQuestion(), 5);
                
                List<String> sourceIds = retrievedChunks.stream()
                        .map(chunk -> String.valueOf(chunk.getId()))
                        .collect(Collectors.toList());

                // 2. 调用AI生成答案
                String answer = generationService.generateAnswerSync(chatRequest.getQuestion(), retrievedChunks);

                // 3. 保存聊天记录到MySQL
                ChatHistory chatHistory = new ChatHistory();
                chatHistory.setUserId(chatRequest.getUserId());
                chatHistory.setQuestion(chatRequest.getQuestion());
                chatHistory.setAnswer(answer);
                chatHistory.setSourceIds(String.join(",", sourceIds));
                chatHistory.setCreateTime(LocalDateTime.now());
                chatHistoryMapper.insert(chatHistory);

                // 4. 保存对话历史到Redis
                String conversationId = chatRequest.getConversationId();
                if (conversationId == null) {
                    conversationId = String.valueOf(System.currentTimeMillis());
                }
                String userIdStr = chatRequest.getUserId() != null ? String.valueOf(chatRequest.getUserId()) : "anonymous";
                chatHistoryService.saveMessage(userIdStr, conversationId, "user", chatRequest.getQuestion(), null);
                chatHistoryService.saveMessage(userIdStr, conversationId, "assistant", answer, sourceIds);

                return new ChatResponse(answer, sourceIds, null, conversationId);
            } catch (IOException e) {
                logger.error("RAG检索失败", e);
                throw new RuntimeException("知识库检索失败", e);
            }
        });
    }

    /**
     * 降级方法 - 当熔断器打开或超时时调用
     */
    public CompletableFuture<ChatResponse> chatFallback(ChatRequest chatRequest, Throwable t) {
        logger.warn("RAG服务触发熔断降级: {}", t.getMessage());
        String fallbackAnswer = "AI服务暂时不可用，请稍后重试。";
        return CompletableFuture.completedFuture(
                new ChatResponse(fallbackAnswer, List.of(), null, chatRequest.getConversationId())
        );
    }

    /**
     * 同步版本的chat方法(保留兼容)
     */
    public Mono<ChatResponse> chat(ChatRequest chatRequest) {
        return Mono.defer(() -> {
            logger.info("开始处理RAG请求: question={}, userId={}, conversationId={}", 
                    chatRequest.getQuestion(), chatRequest.getUserId(), chatRequest.getConversationId());
            
            List<DocumentChunk> retrievedChunks;
            try {
                logger.info("开始检索文档...");
                retrievedChunks = retrievalService.retrieveTopKChunks(chatRequest.getQuestion(), 5);
                logger.info("检索到 {} 个文档片段", retrievedChunks.size());
            } catch (IOException e) {
                logger.error("知识库检索失败", e);
                return Mono.error(new RuntimeException("知识库检索失败，请检查 Elasticsearch 服务", e));
            }

            List<String> sourceIds = retrievedChunks.stream()
                    .map(chunk -> String.valueOf(chunk.getId()))
                    .collect(Collectors.toList());

            logger.info("开始调用AI生成答案...");
            return generationService.generateAnswer(chatRequest.getQuestion(), retrievedChunks)
                    .doOnSuccess(answer -> logger.info("AI生成答案成功"))
                    .doOnError(e -> logger.error("AI生成答案失败", e))
                    .onErrorResume(e -> {
                        String fallbackAnswer = "AI 服务暂时不可用（" + e.getMessage() + "），请稍后重试或检查 DeepSeek API 配置。";
                        return Mono.just(fallbackAnswer);
                    })
                    .flatMap(answer -> {
                        try {
                            logger.info("开始保存聊天记录...");
                            ChatHistory chatHistory = new ChatHistory();
                            try {
                                chatHistory.setUserId(chatRequest.getUserId());
                            } catch (Exception e) {
                                logger.warn("设置userId失败，使用默认值", e);
                                chatHistory.setUserId("anonymous");
                            }
                            chatHistory.setQuestion(chatRequest.getQuestion());
                            chatHistory.setAnswer(answer);
                            chatHistory.setSourceIds(String.join(",", sourceIds));
                            chatHistory.setCreateTime(LocalDateTime.now());
                            
                            logger.info("准备插入MySQL: userId={}, question={}, answer={}", 
                                    chatHistory.getUserId(), chatHistory.getQuestion(), 
                                    answer != null ? answer.substring(0, Math.min(50, answer.length())) : "null");
                            
                            chatHistoryMapper.insert(chatHistory);
                            logger.info("MySQL聊天记录保存成功，ID={}", chatHistory.getId());
                            
                            // 保存对话历史到Redis
                            String conversationId = chatRequest.getConversationId();
                            if (conversationId == null) {
                                conversationId = String.valueOf(System.currentTimeMillis());
                            }
                            String userIdStr = chatRequest.getUserId() != null ? chatRequest.getUserId() : "anonymous";
                            
                            logger.info("准备保存Redis: userId={}, conversationId={}", userIdStr, conversationId);
                            chatHistoryService.saveMessage(userIdStr, conversationId, "user", chatRequest.getQuestion(), null);
                            logger.info("Redis用户消息保存成功");
                            
                            chatHistoryService.saveMessage(userIdStr, conversationId, "assistant", answer, sourceIds);
                            logger.info("Redis AI回复保存成功");
                            
                            logger.info("Redis对话历史保存成功");
                        } catch (Exception e) {
                            logger.error("保存聊天记录失败", e);
                            e.printStackTrace();
                        }

                        return Mono.just(new ChatResponse(answer, sourceIds, null, chatRequest.getConversationId()));
                    });
        });
    }
}