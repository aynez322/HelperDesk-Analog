package com.helpdesk.api.events;

import java.time.LocalDateTime;

/**
 * Internal application event fired when a staff member (agent/manager) replies
 * to a FORM ticket, so the requester can be notified by email.
 *
 * Published inside the reply transaction by {@code TicketService}; forwarded
 * to RabbitMQ (routing key {@code ticket.replied}) only after commit.
 */
public record TicketReplyEvent(
        Long ticketId,
        String subject,
        String requesterEmail,
        LocalDateTime createdAt
) {}