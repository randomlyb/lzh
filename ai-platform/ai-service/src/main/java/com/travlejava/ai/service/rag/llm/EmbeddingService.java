package com.travlejava.ai.service.rag.llm;

import com.travlejava.ai.pojo.llm.deepseek.DeepSeekEmbeddingRequest;
import com.travlejava.ai.pojo.llm.deepseek.DeepSeekEmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 文本向量化服务。
 *
 * <p>注意：早期实现里当 Embedding API 不可用时会返回一个“基于 text.hashCode() 的
 * 伪向量”（768 维）让链路跑通。那个实现有两个严重问题：
 * <ol>
 *   <li>维度与 ES 索引不一致（索引 dims=1024，伪向量 768），索引/检索都会出错；</li>
 *   <li>即使维度对得上，伪向量也没有语义，会静默污染 kNN 召回结果，
 *       而调用方无法区分“真实向量”和“垃圾向量”。</li>
 * </ol>
 * 现在改为：API 失败直接返回 null，由调用方显式降级为纯 BM25 检索。
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private final WebClient webClient;

    @Value("${deepseek.embedding-model:deepseek-embedding}")
    private String embeddingModel;

    /** 期望的向量维度，与 ES dense_vector 的 dims 保持一致 */
    @Value("${deepseek.embedding-dim:1024}")
    private int expectedDim;

    public EmbeddingService(@Value("${deepseek.base-url}") String baseUrl,
                            @Value("${deepseek.api-key}") String apiKey,
                            WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<List<Float>> embed(String text) {
        DeepSeekEmbeddingRequest request = new DeepSeekEmbeddingRequest(embeddingModel, text);
        return webClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepSeekEmbeddingResponse.class)
                .map(response -> {
                    if (response.getData() == null || response.getData().isEmpty()) {
                        throw new RuntimeException("Embedding API returned empty data");
                    }
                    List<Float> vector = response.getData().get(0).getEmbedding();
                    if (vector != null && vector.size() != expectedDim) {
                        // 维度不一致会导致 ES 写入/检索失败，这里提前暴露而不是静默放行
                        logger.warn("Embedding 维度不匹配: 实际 {} 维, 期望 {} 维 (模型 {}). "
                                        + "请检查 deepseek.embedding-model 与 ES 索引 dims 是否一致。",
                                vector.size(), expectedDim, embeddingModel);
                    }
                    return vector;
                })
                .doOnError(e -> logger.error("Embedding 调用失败: error={}", e.getMessage()));
    }

    /**
     * 同步获取向量。
     *
     * @return 成功返回真实向量；失败返回 {@code null}，调用方应降级为纯 BM25 检索。
     */
    public List<Float> embedSync(String text) {
        try {
            return embed(text).block();
        } catch (Exception e) {
            logger.warn("Embedding API 不可用，向量检索本次跳过（降级为纯 BM25）: {}", e.getMessage());
            return null;
        }
    }
}
