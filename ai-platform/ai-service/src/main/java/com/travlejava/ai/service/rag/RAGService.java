package com.travlejava.ai.service.rag;

import com.travlejava.ai.pojo.ai.ChatHistory;
import com.travlejava.ai.pojo.document.DocumentChunk;
import com.travlejava.ai.pojo.rag.dto.ChatRequest;
import com.travlejava.ai.pojo.rag.dto.ChatResponse;
import com.travlejava.ai.service.mapper.ChatHistoryMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RAGService {

    private final ElasticsearchRetrievalService retrievalService;
    private final DeepSeekGenerationService generationService;
    private final ChatHistoryMapper chatHistoryMapper;

    public RAGService(ElasticsearchRetrievalService retrievalService, DeepSeekGenerationService generationService, ChatHistoryMapper chatHistoryMapper) {
        this.retrievalService = retrievalService;
        this.generationService = generationService;
        this.chatHistoryMapper = chatHistoryMapper;
    }

    public Mono<ChatResponse> chat(ChatRequest chatRequest) {
        return Mono.defer(() -> {
            List<DocumentChunk> retrievedChunks;
            try {
                retrievedChunks = retrievalService.retrieveTopKChunks(chatRequest.getQuestion(), 5); // Retrieve top 5 chunks
            } catch (IOException e) {
                return Mono.error(new RuntimeException("Error retrieving documents from Elasticsearch", e));
            }

            List<String> sourceIds = retrievedChunks.stream()
                    .map(chunk -> String.valueOf(chunk.getId()))
                    .collect(Collectors.toList());

            return generationService.generateAnswer(chatRequest.getQuestion(), retrievedChunks)
                    .flatMap(answer -> {
                        // Save chat history
                        ChatHistory chatHistory = new ChatHistory();
                        chatHistory.setUserId(chatRequest.getUserId());
                        chatHistory.setQuestion(chatRequest.getQuestion());
                        chatHistory.setAnswer(answer);
                        chatHistory.setSourceIds(String.join(",", sourceIds)); // Store as comma-separated string
                        chatHistory.setTokenUsage(null); // DeepSeekGenerationService doesn't return token usage directly yet
                        chatHistory.setCreateTime(LocalDateTime.now());
                        chatHistoryMapper.insert(chatHistory);

                        return Mono.just(new ChatResponse(answer, sourceIds, null, chatRequest.getConversationId()));
                    });
        });
    }
}
