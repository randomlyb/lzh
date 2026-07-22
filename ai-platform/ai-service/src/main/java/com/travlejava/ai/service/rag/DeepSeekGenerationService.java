package com.travlejava.ai.service.rag;

import com.travlejava.ai.pojo.document.DocumentChunk;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatRequest;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekChatResponse;
import com.travlejava.ai.pojo.llm.deepseek.Message;
import com.travlejava.ai.service.rag.llm.DeepSeekClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeepSeekGenerationService {

    private final DeepSeekClient deepSeekClient;

    public DeepSeekGenerationService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public Mono<String> generateAnswer(String userQuestion, List<DocumentChunk> retrievedChunks) {
        String prompt = buildPrompt(userQuestion, retrievedChunks);

        DeepSeekChatRequest request = new DeepSeekChatRequest(
                List.of(new Message("user", prompt)),
                "deepseek-chat", // Or your preferred DeepSeek model
                0.7, // Temperature for creativity
                1024, // Max tokens for response
                false // Not streaming
        );

        return deepSeekClient.chatCompletion(request)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }

    private String buildPrompt(String userQuestion, List<DocumentChunk> chunks) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an AI assistant. Answer the following question based on the provided context only.\n\n");

        if (chunks != null && !chunks.isEmpty()) {
            promptBuilder.append("Context:\n");
            for (DocumentChunk chunk : chunks) {
                promptBuilder.append("-").append(chunk.getContent()).append("\n");
            }
            promptBuilder.append("\n");
        }

        promptBuilder.append("Question: ").append(userQuestion);

        return promptBuilder.toString();
    }
}
