package com.helpdesk.api.chat;

import com.helpdesk.api.domain.Ticket;
import com.helpdesk.api.events.TicketCreatedEvent;
import com.helpdesk.api.tickets.MessageResponse;
import com.helpdesk.api.tickets.TicketResponse;
import com.helpdesk.api.tickets.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP handler for live chat. See README section 6.
 *
 * First message (no ticketId): mints a LIVE ticket (URGENT/OPEN), saves the
 * message, broadcasts the ticket to /topic/agents/inbox, then streams the
 * message to /topic/tickets/{id}.
 *
 * Follow-up messages (ticketId set): saved and broadcast to /topic/tickets/{id}.
 * Ownership/staff checks are enforced by TicketService — same rules as REST.
 *
 * The Principal injected here comes from StompAuthChannelInterceptor (JWT on
 * CONNECT). It is already an Authentication with ROLE_* authorities, so
 * TicketService's role checks work unchanged. Anonymous clients → null.
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final TicketService ticketService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public ChatController(TicketService ticketService,
                          SimpMessagingTemplate messagingTemplate,
                          ApplicationEventPublisher eventPublisher) {
        this.ticketService = ticketService;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
    }

    @MessageMapping("/chat.send")
    public void chat(@Payload ChatMessagePayload payload, Principal principal) {
        if (payload.body() == null || payload.body().isBlank()) {
            return;
        }

        Authentication auth = toAuthentication(principal);
        Long ticketId = payload.ticketId();

        if (ticketId == null) {
            Ticket ticket = ticketService.createLiveTicket(payload.body(), payload.requesterEmail(), auth);
            ticketId = ticket.getId();
            log.info("LIVE ticket #{} created via chat (requester={})",
                    ticketId, ticket.getRequesterEmail());

            // New LIVE ticket → agent inbox
            messagingTemplate.convertAndSend("/topic/agents/inbox", TicketResponse.of(ticket));

            // Keep the AMQP event stream consistent with the FORM path
            String notifyEmail = ticket.getRequesterEmail() != null
                    ? ticket.getRequesterEmail()
                    : ticket.getCreatedBy() != null ? ticket.getCreatedBy().getEmail() : null;
            eventPublisher.publishEvent(new TicketCreatedEvent(
                    ticket.getId(),
                    ticket.getSubject(),
                    ticket.getDescription(),
                    notifyEmail,
                    ticket.getType().name(),
                    ticket.getPriority().name(),
                    ticket.getCategory() != null ? ticket.getCategory().getName() : null,
                    ticket.getCreatedAt()));
        }

        MessageResponse message = ticketService.addMessage(ticketId, payload.body(), auth);
        messagingTemplate.convertAndSend("/topic/tickets/" + ticketId, message);
    }

    private Authentication toAuthentication(Principal principal) {
        // The interceptor sets a UsernamePasswordAuthenticationToken (an Authentication);
        // pass it through so authorities are preserved.
        return principal instanceof Authentication auth ? auth : null;
    }
}
