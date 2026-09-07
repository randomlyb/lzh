package com.travlejava.taskscheduler.service.mq;

import com.travlejava.taskscheduler.common.enums.TriggerType;
import com.travlejava.taskscheduler.common.mq.DocumentParseMessage;
import com.travlejava.taskscheduler.service.JobService;
import com.travlejava.taskscheduler.service.config.RabbitMQConfig;
import com.travlejava.taskscheduler.service.dto.JobCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMessageListener.class);

    private final JobService jobService;

    public RabbitMQMessageListener(JobService jobService) {
        this.jobService = jobService;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_PARSE_QUEUE)
    public void handleDocumentParseMessage(DocumentParseMessage message) {
        logger.info("Received document parse message: {}", message);
        // Create a new job to parse the document
        JobCreateRequest jobCreateRequest = new JobCreateRequest();
        jobCreateRequest.setJobName("Document Parse Job for DocId: " + message.getDocumentId());
        jobCreateRequest.setExecutorHandler("pdfParseJobExecutor");
        
        // Build JSON params with all metadata - escape backslashes for Windows paths
        String escapedPath = message.getDocumentPath().replace("\\", "\\\\");
        String executorParams = String.format(
                "{\"documentId\":%d,\"filePath\":\"%s\",\"title\":\"%s\",\"fileType\":\"%s\",\"description\":\"%s\",\"knowledgeBaseId\":%s,\"uploadTime\":\"%s\"}", 
                message.getDocumentId(), 
                escapedPath,
                message.getTitle() != null ? message.getTitle().replace("\"", "\\\"") : "",
                message.getFileType() != null ? message.getFileType() : "",
                message.getDescription() != null ? message.getDescription().replace("\"", "\\\"") : "",
                message.getKnowledgeBaseId() != null ? message.getKnowledgeBaseId() : "null",
                message.getUploadTime() != null ? message.getUploadTime() : ""
        );
        jobCreateRequest.setExecutorParams(executorParams);
        jobCreateRequest.setTriggerType(TriggerType.FIXED_RATE);
        jobCreateRequest.setFixedRate(1000L);
        jobCreateRequest.setMaxRetryCount(3);

        try {
            jobService.createJob(jobCreateRequest);
            logger.info("Created and scheduled job for document ID: {}", message.getDocumentId());
        } catch (Exception e) {
            logger.error("Failed to create/schedule job for document ID: {}", message.getDocumentId(), e);
        }
    }
}