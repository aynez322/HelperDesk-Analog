package com.helpdesk.api.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;

    @Test
    void publishesToHelpdeskEventsExchangeWithTicketCreatedRoutingKey() {
        TicketEventPublisher publisher = new TicketEventPublisher(rabbitTemplate);
        TicketCreatedEvent event = new TicketCreatedEvent(
                1L, "Subject", "Desc", "a@example.com", "FORM", "NORMAL", null, LocalDateTime.now());

        publisher.onTicketCreated(event);

        verify(rabbitTemplate).convertAndSend(
                "helpdesk.events", "ticket.created", event);
    }

    @Test
    void propagatesNothingWhenBrokerDown() {
        // By design a broker outage must not fail the (already committed)
        // ticket creation — the publisher swallows the exception and logs.
        TicketEventPublisher publisher = new TicketEventPublisher(rabbitTemplate);
        TicketCreatedEvent event = new TicketCreatedEvent(
                2L, "S", "D", "a@example.com", "FORM", "NORMAL", null, LocalDateTime.now());

        doThrow(new AmqpException("connection refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Should not throw
        publisher.onTicketCreated(event);
    }
}
