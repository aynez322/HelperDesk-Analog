package com.helpdesk.api.events;

import java.time.LocalDateTime;

/**
 * Internal application event fired when a new ticket is created.
 *
 * Published inside the ticket-creation transaction by {@code TicketService};
 * forwarded to RabbitMQ (exchange {@code helpdesk.events}, routing key
 * {@code ticket.created}) only after the transaction commits, so no event
 * is ever sent for a ticket that failed to persist.
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
