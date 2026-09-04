package com.helpdesk.api.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE_HELPDESK_EVENTS = "helpdesk.events";
    public static final String QUEUE_NOTIFICATIONS_EMAIL = "notifications.email";
    public static final String QUEUE_NOTIFICATIONS_EMAIL_REPLY = "notifications.email.reply";
    public static final String ROUTING_KEY_TICKET_CREATED = "ticket.created";
    public static final String ROUTING_KEY_TICKET_REPLIED = "ticket.replied";
    public static final String DLX_HELPDESK_EVENTS = "helpdesk.events.dlx";

    @Bean
    public TopicExchange helpdeskEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_HELPDESK_EVENTS).durable(true).build();
    }

    @Bean
    public TopicExchange helpdeskEventsDlx() {
        return ExchangeBuilder.topicExchange(DLX_HELPDESK_EVENTS).durable(true).build();
    }

    @Bean
    public Queue notificationsEmailQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS_EMAIL)
                .deadLetterExchange(DLX_HELPDESK_EVENTS)
                .build();
    }

    @Bean
    public Queue notificationsEmailReplyQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS_EMAIL_REPLY)
                .deadLetterExchange(DLX_HELPDESK_EVENTS)
                .build();
    }

    @Bean
    public Binding notificationsEmailBinding(TopicExchange helpdeskEventsExchange,
                                             Queue notificationsEmailQueue) {
        return BindingBuilder.bind(notificationsEmailQueue)
                .to(helpdeskEventsExchange)
                .with(ROUTING_KEY_TICKET_CREATED);
    }

    @Bean
    public Binding notificationsEmailReplyBinding(TopicExchange helpdeskEventsExchange,
                                                  Queue notificationsEmailReplyQueue) {
        return BindingBuilder.bind(notificationsEmailReplyQueue)
                .to(helpdeskEventsExchange)
                .with(ROUTING_KEY_TICKET_REPLIED);
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
