package com.travlejava.ai.service.mq;

import com.travlejava.ai.service.config.RabbitMQConfig;
import com.travlejava.taskscheduler.common.mq.DocumentParseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(DocumentEventProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public DocumentEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendDocumentParseMessage(DocumentParseMessage message) {
        logger.info("Sending document parse message: {}", message);
        rabbitTemplate.convertAndSend(RabbitMQConfig.DOCUMENT_PARSE_EXCHANGE,
                                      RabbitMQConfig.DOCUMENT_PARSE_ROUTING_KEY,
                                      message);
    }
}
