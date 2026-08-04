package com.helpdesk.api.tickets;

import com.helpdesk.api.domain.Message;
import com.helpdesk.api.domain.SenderType;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long ticketId,
        Long senderId,
        String senderEmail,
        SenderType senderType,
        String body,
        LocalDateTime createdAt
) {
    public static MessageResponse of(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getTicket().getId(),
                m.getSender() != null ? m.getSender().getId() : null,
                m.getSender() != null ? m.getSender().getEmail() : null,
                m.getSenderType(),
                m.getBody(),
                m.getCreatedAt()
        );
    }
}