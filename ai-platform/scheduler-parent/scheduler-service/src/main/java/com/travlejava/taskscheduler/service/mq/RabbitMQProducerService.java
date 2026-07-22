package com.travlejava.taskscheduler.service.mq;

import com.travlejava.taskscheduler.common.mq.JobStatusNotification;
import com.travlejava.taskscheduler.service.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQProducerService {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQProducerService.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendJobStatusNotification(JobStatusNotification notification) {
        logger.info("Sending job status notification: {}", notification);
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_SERVICE_NOTIFICATION_EXCHANGE,
                                      RabbitMQConfig.AI_SERVICE_NOTIFICATION_ROUTING_KEY,
                                      notification);
    }
}
