package com.helpdesk.notifications.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ consumer topology for the Notifications microservice.
 *
 * Mirrors the backend's {@code RabbitTopologyConfig} declaration (durable topic
 * exchange {@code helpdesk.events}, durable queue {@code notifications.email},
 * binding {@code ticket.created}). Declarations are idempotent: re-declaring
 * the identical topology on a running broker is a no-op. The consumer declares
 * it too so events buffer in the queue even if the backend has never connected.
 *
 * Dead-lettering: the main queue carries {@code x-dead-letter-exchange} pointing
 * at {@code helpdesk.events.dlx}. When a message exhausts the in-memory retries
 * (see application.yml retry config) and is rejected with {@code requeue=false},
 * the broker routes it to the DLX → {@code notifications.email.dlq} instead of
 * dropping it — so nothing is silently lost. A human/alert can inspect the DLQ.
 *
 * Message deserialization: the backend publishes JSON with a
 * {@code __TypeId__: com.helpdesk.api.events.TicketCreatedEvent} header (the
 * producer's FQCN). This service does not share code with the backend, so the
 * type mapper maps that type id to the local mirror record — see
 * {@link com.helpdesk.notifications.events.TicketCreatedEvent}.
 */
@Configuration
public class RabbitConsumerConfig {

    public static final String EXCHANGE_HELPDESK_EVENTS = "helpdesk.events";
    public static final String QUEUE_NOTIFICATIONS_EMAIL = "notifications.email";
    public static final String ROUTING_KEY_TICKET_CREATED = "ticket.created";

    public static final String DLX_HELPDESK_EVENTS = "helpdesk.events.dlx";
    public static final String DLQ_NOTIFICATIONS_EMAIL = "notifications.email.dlq";

    /** Type id written by the backend's Jackson2JsonMessageConverter. */
    public static final String PRODUCER_TICKET_CREATED_TYPE = "com.helpdesk.api.events.TicketCreatedEvent";

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
                // Messages that fail all retries are routed to the DLX → DLQ.
                .deadLetterExchange(DLX_HELPDESK_EVENTS)
                .build();
    }

    @Bean
    public Queue notificationsEmailDlq() {
        return QueueBuilder.durable(DLQ_NOTIFICATIONS_EMAIL).build();
    }

    @Bean
    public Binding notificationsEmailBinding(TopicExchange helpdeskEventsExchange,
                                             Queue notificationsEmailQueue) {
        return BindingBuilder.bind(notificationsEmailQueue)
                .to(helpdeskEventsExchange)
                .with(ROUTING_KEY_TICKET_CREATED);
    }

    @Bean
    public Binding notificationsEmailDlqBinding(TopicExchange helpdeskEventsDlx,
                                                Queue notificationsEmailDlq) {
        // DLX uses the same routing key so the original "ticket.created" key is preserved.
        return BindingBuilder.bind(notificationsEmailDlq)
                .to(helpdeskEventsDlx)
                .with(ROUTING_KEY_TICKET_CREATED);
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setIdClassMapping(Map.of(
                PRODUCER_TICKET_CREATED_TYPE,
                com.helpdesk.notifications.events.TicketCreatedEvent.class));
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
