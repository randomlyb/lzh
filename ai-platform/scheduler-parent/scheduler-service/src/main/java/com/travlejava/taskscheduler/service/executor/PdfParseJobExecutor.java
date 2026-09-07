package com.travlejava.taskscheduler.service.executor;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.taskscheduler.common.entity.DocumentMetadata;
import com.travlejava.taskscheduler.common.executor.Executor;
import com.travlejava.taskscheduler.common.model.Result;
import com.travlejava.taskscheduler.common.model.TaskExecuteParam;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component("pdfParseJobExecutor")
public class PdfParseJobExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(PdfParseJobExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DOCUMENT_CHUNK_INDEX = "document_chunks";
    private static final int CHUNK_SIZE = 500;
    private static final String LOCK_KEY_PREFIX = "lock:pdf_parse:";
    private static final long LOCK_WAIT_TIME = 5;
    private static final long LOCK_LEASE_TIME = 600;
    private static final int PDF_PARSE_TIMEOUT_SECONDS = 300;
    private static final int MAX_PDF_PAGES = 500;
    private static final long MAX_PDF_SIZE_MB = 100;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Result<String> execute(TaskExecuteParam param) {
        logger.info("开始执行文档解析任务，参数: {}", param.getExecutorParams());
        
        Long documentId = parseDocumentId(param.getExecutorParams());
        if (documentId == null) {
            return Result.error("缺少文档ID参数");
        }

        String lockKey = LOCK_KEY_PREFIX + documentId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                logger.info("成功获取分布式锁，开始解析文档: {}", documentId);
                return doExecute(param);
            } else {
                logger.warn("获取分布式锁失败，文档 {} 正在被其他实例解析", documentId);
                return Result.error("文档正在解析中，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取分布式锁被中断: {}", documentId, e);
            return Result.error("任务执行被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.info("释放分布式锁: {}", documentId);
            }
        }
    }

    private Long parseDocumentId(String executorParams) {
        try {
            if (executorParams != null && executorParams.trim().startsWith("{")) {
                JsonNode params = objectMapper.readTree(executorParams);
                return params.has("documentId") ? params.get("documentId").asLong() : null;
            }
        } catch (Exception e) {
            logger.error("解析文档ID失败", e);
        }
        return null;
    }

    private Result<String> doExecute(TaskExecuteParam param) {
        try {
            Long documentId = parseDocumentId(param.getExecutorParams());
            String filePath = parseFilePath(param.getExecutorParams());
            String title = parseTitle(param.getExecutorParams());
            String fileType = parseFileType(param.getExecutorParams());
            String description = parseDescription(param.getExecutorParams());
            Long knowledgeBaseId = parseKnowledgeBaseId(param.getExecutorParams());
            LocalDateTime uploadTime = parseUploadTime(param.getExecutorParams());

            if (filePath == null) {
                return Result.error("缺少文件路径参数");
            }

            File docFile = new File(filePath);
            if (!docFile.exists()) {
                return Result.error("文件不存在: " + filePath);
            }

            long fileSizeMB = docFile.length() / (1024 * 1024);
            if (fileSizeMB > MAX_PDF_SIZE_MB) {
                return Result.error("文件过大: " + fileSizeMB + "MB，最大支持" + MAX_PDF_SIZE_MB + "MB");
            }

            DocumentMetadata metadata = new DocumentMetadata();
            metadata.setId(documentId);
            metadata.setFilePath(filePath);
            metadata.setTitle(title != null ? title : "Unknown");
            metadata.setFileType(fileType);
            metadata.setDescription(description);
            metadata.setKnowledgeBaseId(knowledgeBaseId);
            metadata.setUploadTime(uploadTime);

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return extractTextFromFile(docFile, metadata.getFileType());
                } catch (IOException e) {
                    throw new RuntimeException("文档解析失败", e);
                }
            });

            String text = future.get(PDF_PARSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (text == null || text.trim().isEmpty()) {
                return Result.error("文档内容为空");
            }

            List<String> chunks = splitIntoChunks(text, CHUNK_SIZE);
            indexChunksToElasticsearch(metadata, chunks);

            logger.info("文档解析完成: {}, 文本块数量: {}", metadata.getTitle(), chunks.size());
            return Result.success("文档解析完成，共生成 " + chunks.size() + " 个文本块");

        } catch (TimeoutException e) {
            logger.error("文档解析超时（{}秒）", PDF_PARSE_TIMEOUT_SECONDS);
            return Result.error("文档解析超时");
        } catch (Exception e) {
            logger.error("文档解析失败", e);
            return Result.error("文档解析失败: " + e.getMessage());
        }
    }

    private String parseFilePath(String executorParams) {
        try {
            if (executorParams != null && executorParams.trim().startsWith("{")) {
                JsonNode params = objectMapper.readTree(executorParams);
                return params.has("filePath") ? params.get("filePath").asText() : null;
            } else if (executorParams != null && !executorParams.trim().isEmpty()) {
                return executorParams.trim();
            }
        } catch (Exception e) {
            logger.error("解析文件路径失败", e);
        }
        return null;
    }

    private String parseTitle(String executorParams) {
        try {
            if (executorParams != null && executorParams.trim().startsWith("{")) {
                JsonNode params = objectMapper.readTree(executorParams);
                return params.has("title") ? params.get("title").asText() : null;
            }
        } catch (Exception e) {
            logger.error("解析标题失败", e);
        }
        return null;
    }

    private String parseFileType(String executorParams) {
        try {
            if (executorParams != null && executorParams.trim().startsWith("{")) {
                JsonNode params = objectMapper.readTree(executorParams);
                return params.has("fileType") ? params.get("fileType").asText() : null;
            }
        } catch (Exception e) {
            logger.error("解析文件类型失败", e);
        }
        return null;
    }

    private String parseDescription(String executorParams) {
        try {
            if (executorParams != null && executorParams.trim().startsWith("{")) {
                JsonNode params = objectMapper.readTree(executorParams);
                return params.has("description") ? params.get("description").asText() : null;
            }
        } catch (Exception e) {
            logger.error("解析描述失败", e);
        }
        return null;
    }

    private Long parseKnowledgeBaseId(String executorParams) {
        try {
            if (executorParams != null && executorParams.trim().startsWith("{")) {
                JsonNode params = objectMapper.readTree(executorParams);
                return params.has("knowledgeBaseId") && !params.get("knowledgeBaseId").isNull() 
                        ? params.get("knowledgeBaseId").asLong() : null;
            }
        } catch (Exception e) {
            logger.error("解析知识库ID失败", e);
        }
        return null;
    }

    private LocalDateTime parseUploadTime(String executorParams) {
        try {
            if (executorParams != null && executorParams.trim().startsWith("{")) {
                JsonNode params = objectMapper.readTree(executorParams);
                if (params.has("uploadTime") && !params.get("uploadTime").isNull()) {
                    return LocalDateTime.parse(params.get("uploadTime").asText());
                }
            }
        } catch (Exception e) {
            logger.error("解析上传时间失败", e);
        }
        return null;
    }

    private String extractTextFromFile(File file, String fileType) throws IOException {
        if (fileType == null) {
            fileType = "";
        }
        String lowerType = fileType.toLowerCase();

        if (lowerType.contains("pdf") || file.getName().toLowerCase().endsWith(".pdf")) {
            return extractTextFromPdf(file);
        } else if (lowerType.contains("word") || lowerType.contains("docx") || file.getName().toLowerCase().endsWith(".docx")) {
            return extractTextFromDocx(file);
        } else if (lowerType.contains("text") || lowerType.contains("plain") || file.getName().toLowerCase().endsWith(".txt")) {
            return extractTextFromTxt(file);
        } else {
            return extractTextFromTxt(file);
        }
    }

    private String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile, 
                org.apache.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly())) {
            
            int pageCount = document.getNumberOfPages();
            if (pageCount > MAX_PDF_PAGES) {
                throw new IOException("PDF页数过多: " + pageCount + "页，最大支持" + MAX_PDF_PAGES + "页");
            }

            logger.info("开始解析PDF: {} 页", pageCount);
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            StringBuilder textBuilder = new StringBuilder();
            int batchSize = 10;
            
            for (int startPage = 1; startPage <= pageCount; startPage += batchSize) {
                int endPage = Math.min(startPage + batchSize - 1, pageCount);
                stripper.setStartPage(startPage);
                stripper.setEndPage(endPage);
                
                String batchText = stripper.getText(document);
                textBuilder.append(batchText);
                
                logger.debug("已解析PDF页码: {}/{}", endPage, pageCount);
            }
            
            return textBuilder.toString();
        }
    }

    private String extractTextFromDocx(File docxFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(docxFile);
             XWPFDocument document = new XWPFDocument(fis)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    private String extractTextFromTxt(File txtFile) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(txtFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
    }

    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }

            if (currentChunk.length() + trimmedParagraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(trimmedParagraph).append("\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private void indexChunksToElasticsearch(DocumentMetadata metadata, List<String> chunks) throws IOException {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = metadata.getId() + "_chunk_" + i;
            Map<String, Object> chunkDoc = new HashMap<>();
            chunkDoc.put("id", chunkId);
            chunkDoc.put("documentId", String.valueOf(metadata.getId()));
            chunkDoc.put("documentTitle", metadata.getTitle());
            chunkDoc.put("content", chunks.get(i));
            chunkDoc.put("chunkIndex", i);
            chunkDoc.put("totalChunks", chunks.size());
            chunkDoc.put("createTime", metadata.getUploadTime() != null ? metadata.getUploadTime().toString() : LocalDateTime.now().toString());

            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(DOCUMENT_CHUNK_INDEX)
                            .id(chunkId)
                            .document(chunkDoc)
                    )
            );
        }

        BulkRequest bulkRequest = bulkBuilder.build();
        BulkResponse bulkResponse = esClient.bulk(bulkRequest);

        if (bulkResponse.errors()) {
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    logger.error("文本块索引失败: {} - {}", item.id(), item.error().reason());
                }
            }
        } else {
            logger.info("成功索引 {} 个文本块到 ES", chunks.size());
        }
    }

}