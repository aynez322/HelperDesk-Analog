package com.helpdesk.notifications.events;

import java.time.LocalDateTime;

/**
 * Local mirror of the backend's {@code com.helpdesk.api.events.TicketCreatedEvent}
 * — the JSON payload published on exchange {@code helpdesk.events} with routing
 * key {@code ticket.created}. Field names must match the producer's record
 * exactly. Mapped in {@code RabbitConsumerConfig} via the producer's
 * {@code __TypeId__} header.
 */
public record TicketCreatedEvent(
        Long ticketId,
        String subject,
        String description,
        String requesterEmail,
        String type,
        String priority,
        String category,
        LocalDateTime createdAt
) {}
