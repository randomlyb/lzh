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
        // Here, we create a new job to parse the document.
        // In a real scenario, you might have pre-defined job templates.
        JobCreateRequest jobCreateRequest = new JobCreateRequest();
        jobCreateRequest.setJobName("Document Parse Job for DocId: " + message.getDocumentId());
        jobCreateRequest.setExecutorHandler("pdfParseJobExecutor"); // The bean name of our executor
        jobCreateRequest.setExecutorParams(message.getDocumentPath()); // Pass document path as param
        jobCreateRequest.setTriggerType(TriggerType.FIXED_RATE); // Trigger immediately
        jobCreateRequest.setFixedRate(1000L); // Execute once after 1 second (or a very short interval then unschedule)
        jobCreateRequest.setMaxRetryCount(3);

        try {
            jobService.createJob(jobCreateRequest);
            logger.info("Created and scheduled job for document ID: {}", message.getDocumentId());
        } catch (Exception e) {
            logger.error("Failed to create/schedule job for document ID: {}", message.getDocumentId(), e);
        }
    }
}
