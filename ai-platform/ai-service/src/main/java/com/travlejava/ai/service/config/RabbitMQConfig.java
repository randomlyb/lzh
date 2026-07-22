package com.travlejava.ai.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DOCUMENT_PARSE_QUEUE = "document.parse.queue";
    public static final String DOCUMENT_PARSE_EXCHANGE = "document.parse.exchange";
    public static final String DOCUMENT_PARSE_ROUTING_KEY = "document.parse";

    public static final String AI_SERVICE_NOTIFICATION_QUEUE = "ai.service.notification.queue";
    public static final String AI_SERVICE_NOTIFICATION_EXCHANGE = "ai.service.notification.exchange";
    public static final String AI_SERVICE_NOTIFICATION_ROUTING_KEY = "ai.service.notification";

    // For sending document parse requests to scheduler
    @Bean
    public DirectExchange documentParseExchange() {
        return new DirectExchange(DOCUMENT_PARSE_EXCHANGE);
    }

    // For receiving job status notifications from scheduler
    @Bean
    public Queue aiServiceNotificationQueue() {
        return new Queue(AI_SERVICE_NOTIFICATION_QUEUE, true);
    }

    @Bean
    public DirectExchange aiServiceNotificationExchange() {
        return new DirectExchange(AI_SERVICE_NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding aiServiceNotificationBinding(Queue aiServiceNotificationQueue, DirectExchange aiServiceNotificationExchange) {
        return BindingBuilder.bind(aiServiceNotificationQueue).to(aiServiceNotificationExchange).with(AI_SERVICE_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
