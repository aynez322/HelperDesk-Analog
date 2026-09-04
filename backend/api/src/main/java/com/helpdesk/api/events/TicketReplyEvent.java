package com.helpdesk.api.events;

import java.time.LocalDateTime;

public record TicketReplyEvent(
        Long ticketId,
        String subject,
        String requesterEmail,
        LocalDateTime createdAt
) {}
