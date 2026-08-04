package com.helpdesk.api.tickets;

import com.helpdesk.api.domain.TicketPriority;
import com.helpdesk.api.domain.TicketStatus;
import com.helpdesk.api.domain.TicketType;

import java.time.LocalDateTime;

public record TicketSummary(
        Long id,
        String subject,
        TicketType type,
        TicketStatus status,
        TicketPriority priority,
        String categoryName,
        String assignedToEmail,
        String requesterEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}