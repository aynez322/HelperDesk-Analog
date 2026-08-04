package com.helpdesk.api.tickets;

import com.helpdesk.api.domain.TicketPriority;
import com.helpdesk.api.domain.TicketStatus;

public record UpdateTicketRequest(
        TicketStatus status,
        TicketPriority priority,
        Long assignedToId
) {
}