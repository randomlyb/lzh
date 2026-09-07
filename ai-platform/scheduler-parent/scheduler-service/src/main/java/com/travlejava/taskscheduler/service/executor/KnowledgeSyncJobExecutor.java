package com.travlejava.taskscheduler.service.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travlejava.taskscheduler.common.executor.Executor;
import com.travlejava.taskscheduler.common.model.Result;
import com.travlejava.taskscheduler.common.model.TaskExecuteParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component("knowledgeSyncJobExecutor")
public class KnowledgeSyncJobExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeSyncJobExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("externalRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public Result<String> execute(TaskExecuteParam param) {
        logger.info("开始执行知识库同步任务（GitHub仓库），参数: {}", param.getExecutorParams());
        try {
            JsonNode params = objectMapper.readTree(param.getExecutorParams());
            String repoUrl = params.has("repoUrl") ? params.get("url").asText() : null;
            String branch = params.has("branch") ? params.get("branch").asText() : "main";
            Long knowledgeBaseId = params.has("knowledgeBaseId") ? params.get("knowledgeBaseId").asLong() : 1L;

            if (repoUrl == null || repoUrl.isEmpty()) {
                return Result.error("缺少必要参数: repoUrl");
            }

            String githubApiUrl = convertToApiUrl(repoUrl, branch);
            List<Map<String, String>> files = fetchRepoFiles(githubApiUrl);

            if (files.isEmpty()) {
                return Result.success("仓库中没有需要同步的文件");
            }

            int successCount = 0;
            for (Map<String, String> file : files) {
                try {
                    String content = fetchFileContent(file.get("url"));
                    if (content != null && !content.isEmpty()) {
                        syncToKnowledge(file.get("name"), content, knowledgeBaseId);
                        successCount++;
                        logger.info("文件 [{}] 同步到知识库成功", file.get("name"));
                    }
                } catch (Exception e) {
                    logger.error("文件 [{}] 同步到知识库失败: {}", file.get("name"), e.getMessage());
                }
            }

            logger.info("知识库同步完成，成功 {} 个文件", successCount);
            return Result.success("知识库同步完成，成功 " + successCount + " 个文件");
        } catch (Exception e) {
            logger.error("知识库同步失败", e);
            return Result.error("知识库同步失败: " + e.getMessage());
        }
    }

    private String convertToApiUrl(String repoUrl, String branch) {
        String repoPath = repoUrl.replaceAll("^https://github\\.com/", "");
        return "https://api.github.com/repos/" + repoPath + "/contents?ref=" + branch;
    }

    private List<Map<String, String>> fetchRepoFiles(String apiUrl) {
        List<Map<String, String>> files = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "TaskScheduler");

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (JsonNode item : response.getBody()) {
                    if ("file".equals(item.get("type").asText())) {
                        Map<String, String> file = new HashMap<>();
                        file.put("name", item.get("name").asText());
                        file.put("url", item.get("url").asText());
                        files.add(file);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("获取仓库文件列表失败: {}", e.getMessage());
        }
        return files;
    }

    private String fetchFileContent(String fileUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "TaskScheduler");

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    fileUrl, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode body = response.getBody();
                if (body.has("content")) {
                    String base64Content = body.get("content").asText();
                    return new String(Base64.getDecoder().decode(base64Content));
                }
            }
        } catch (Exception e) {
            logger.error("获取文件内容失败: {}", e.getMessage());
        }
        return null;
    }

    private void syncToKnowledge(String fileName, String content, Long knowledgeBaseId) {
        Map<String, Object> body = new HashMap<>();
        body.put("knowledgeBaseId", knowledgeBaseId);
        body.put("title", fileName);
        body.put("content", content);
        body.put("source", "github");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(toJson(body), headers);

        restTemplate.postForEntity(
                "http://localhost:8083/api/knowledge-bases/add-documents",
                entity,
                String.class
        );
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}