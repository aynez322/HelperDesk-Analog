package com.helpdesk.api.chat;

/**
 * Inbound STOMP message on /app/chat.send.
 *
 * ticketId present     → append to existing LIVE ticket (auth/ownership enforced).
 * ticketId absent      → start a new LIVE ticket; requesterEmail required when
 *                        the connection is anonymous, ignored when authenticated.
 */
public record ChatMessagePayload(
        Long ticketId,
        String body,
        String requesterEmail
) {}
