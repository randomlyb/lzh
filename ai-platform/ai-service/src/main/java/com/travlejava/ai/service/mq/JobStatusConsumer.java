package com.travlejava.ai.service.mq;

import com.travlejava.ai.service.config.RabbitMQConfig;
import com.travlejava.taskscheduler.common.mq.JobStatusNotification;
import com.travlejava.ai.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class JobStatusConsumer {

    private static final Logger logger = LoggerFactory.getLogger(JobStatusConsumer.class);

    private final DocumentService documentService;

    public JobStatusConsumer(DocumentService documentService) {
        this.documentService = documentService;
    }

    @RabbitListener(queues = RabbitMQConfig.AI_SERVICE_NOTIFICATION_QUEUE)
    public void handleJobStatusNotification(JobStatusNotification notification) {
        logger.info("Received job status notification: {}", notification);
        if (notification.getDocumentId() != null) {
            documentService.updateDocumentStatus(notification.getDocumentId(), notification.getStatus(), notification.getMessage());
            logger.info("Document ID: {}, Status: {}, Message: {}", notification.getDocumentId(), notification.getStatus(), notification.getMessage());
        } else {
            logger.warn("Received job status notification without document ID: {}", notification);
        }
    }
}
