package com.travlejava.taskscheduler.service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AiServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestTemplate restTemplate;

    public AiServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void notifyTaskCompleted(Long documentId, String jobId, String status, int chunkCount) {
        try {
            String url = "http://localhost:8083/api/task/notify";
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("documentId", documentId);
            body.put("jobId", jobId);
            body.put("status", status);
            body.put("chunkCount", chunkCount);
            restTemplate.postForObject(url, body, String.class);
            logger.info("Notified AI service: documentId={}, jobId={}, status={}", documentId, jobId, status);
        } catch (Exception e) {
            logger.error("Failed to notify AI service", e);
        }
    }
}