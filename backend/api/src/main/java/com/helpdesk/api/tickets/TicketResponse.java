package com.helpdesk.api.tickets;

import com.helpdesk.api.domain.Ticket;
import com.helpdesk.api.domain.TicketPriority;
import com.helpdesk.api.domain.TicketStatus;
import com.helpdesk.api.domain.TicketType;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String subject,
        String description,
        TicketType type,
        TicketStatus status,
        TicketPriority priority,
        Long categoryId,
        String categoryName,
        Long createdById,
        String createdByEmail,
        Long assignedToId,
        String assignedToEmail,
        String requesterEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt
) {
    public static TicketResponse of(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getSubject(),
                t.getDescription(),
                t.getType(),
                t.getStatus(),
                t.getPriority(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getCreatedBy() != null ? t.getCreatedBy().getId() : null,
                t.getCreatedBy() != null ? t.getCreatedBy().getEmail() : null,
                t.getAssignedTo() != null ? t.getAssignedTo().getId() : null,
                t.getAssignedTo() != null ? t.getAssignedTo().getEmail() : null,
                t.getRequesterEmail(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getClosedAt()
        );
    }
}