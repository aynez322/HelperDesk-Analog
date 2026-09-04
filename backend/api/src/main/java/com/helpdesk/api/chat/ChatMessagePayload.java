package com.helpdesk.api.chat;

public record ChatMessagePayload(
        Long ticketId,
        String body,
        String requesterEmail
) {}
