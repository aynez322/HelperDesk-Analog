package com.helpdesk.notifications.events;

import java.time.LocalDateTime;

/**
 * Local mirror of the backend's {@code com.helpdesk.api.events.TicketReplyEvent}
 * — the JSON payload published on exchange {@code helpdesk.events} with routing
 * key {@code ticket.replied}. Field names must match the producer's record
 * exactly. Mapped in {@code RabbitConsumerConfig} via the producer's
 * {@code __TypeId__} header.
 */
public record TicketReplyEvent(
        Long ticketId,
        String subject,
        String requesterEmail,
        LocalDateTime createdAt
) {}