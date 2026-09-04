package com.helpdesk.api.events;

import java.time.LocalDateTime;

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
