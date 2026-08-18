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

/**
 * RabbitMQ topology for the helpdesk event stream.
 *
 * The publisher declares the full topology (durable topic exchange, durable
 * queue, binding) so that {@code ticket.created} events are buffered by the
 * broker even while the Notifications consumer is not running. Declarables
 * are auto-declared by Spring's RabbitAdmin on first connection.
 *
 * Topology (see README section 7):
 *   exchange:  helpdesk.events      (topic, durable)
 *   queue:     notifications.email  (durable)
 *   routing:   ticket.created
 */
@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE_HELPDESK_EVENTS = "helpdesk.events";
    public static final String QUEUE_NOTIFICATIONS_EMAIL = "notifications.email";
    public static final String ROUTING_KEY_TICKET_CREATED = "ticket.created";

    @Bean
    public TopicExchange helpdeskEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_HELPDESK_EVENTS).durable(true).build();
    }

    @Bean
    public Queue notificationsEmailQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS_EMAIL).build();
    }

    @Bean
    public Binding notificationsEmailBinding(TopicExchange helpdeskEventsExchange,
                                             Queue notificationsEmailQueue) {
        return BindingBuilder.bind(notificationsEmailQueue)
                .to(helpdeskEventsExchange)
                .with(ROUTING_KEY_TICKET_CREATED);
    }

    /**
     * JSON message conversion using Spring Boot's auto-configured ObjectMapper
     * (JavaTimeModule registered), so {@code LocalDateTime} fields serialize
     * as ISO-8601 strings.
     */
    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
