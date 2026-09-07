package com.travlejava.taskscheduler.service.executor;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.taskscheduler.common.executor.Executor;
import com.travlejava.taskscheduler.common.model.Result;
import com.travlejava.taskscheduler.common.model.TaskExecuteParam;
import com.travlejava.taskscheduler.service.client.AiServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("chunkSplitJobExecutor")
public class ChunkSplitJobExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(ChunkSplitJobExecutor.class);
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP_SIZE = 50;
    private static final String INDEX_NAME = "document_chunks";

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private AiServiceClient aiServiceClient;

    @Override
    public Result<String> execute(TaskExecuteParam param) {
        logger.info("开始执行文本切片任务，参数: {}", param.getExecutorParams());
        try {
            JsonNode params = new ObjectMapper().readTree(param.getExecutorParams());
            
            if (params.get("documentId") == null) {
                return Result.error("缺少必要参数: documentId");
            }
            if (params.get("content") == null || params.get("content").asText().isEmpty()) {
                return Result.error("缺少必要参数: content，或content为空");
            }
            
            Long documentId = params.get("documentId").asLong();
            String content = params.get("content").asText();

            List<Map<String, Object>> chunks = splitChunks(content);
            int indexedCount = indexToElasticsearch(documentId, chunks);

            aiServiceClient.notifyTaskCompleted(documentId, String.valueOf(param.getJobId()), "SUCCESS", indexedCount);

            logger.info("文本切片完成，文档ID: {}, 共生成 {} 个片段", documentId, indexedCount);
            return Result.success("文本切片完成，共生成 " + indexedCount + " 个片段");
        } catch (Exception e) {
            logger.error("文本切片失败", e);
            return Result.error("文本切片失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> splitChunks(String content) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        int pos = 0;
        int chunkOrder = 1;
        while (pos < content.length()) {
            int end = Math.min(pos + CHUNK_SIZE, content.length());
            String chunkText = content.substring(pos, end);

            Map<String, Object> chunk = new HashMap<>();
            chunk.put("chunkOrder", chunkOrder);
            chunk.put("content", chunkText);
            chunk.put("tokenCount", chunkText.length());
            chunks.add(chunk);

            logger.debug("已生成第 {} 个片段，位置: {}-{}, 长度: {}", chunkOrder, pos, end, chunkText.length());
            
            // Move to next position with overlap
            pos = end - OVERLAP_SIZE;
            chunkOrder++;
            
            // If content is shorter than CHUNK_SIZE, we're done after first chunk
            if (pos <= 0 || pos >= content.length()) {
                break;
            }
        }
        return chunks;
    }

    private int indexToElasticsearch(Long documentId, List<Map<String, Object>> chunks) throws Exception {
        if (chunks.isEmpty()) {
            return 0;
        }

        List<BulkOperation> operations = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> chunk = chunks.get(i);
            chunk.put("documentId", documentId);

            String docId = documentId + "_" + chunk.get("chunkOrder");
            operations.add(BulkOperation.of(op -> op
                    .index(idx -> idx
                            .index(INDEX_NAME)
                            .id(docId)
                            .document(chunk)
                    )
            ));
        }

        var response = esClient.bulk(b -> b.operations(operations));
        int successCount = (int) response.items().stream()
                .filter(item -> item.error() == null)
                .count();

        logger.info("成功索引 {} 个文档片段到 Elasticsearch", successCount);
        return successCount;
    }
}