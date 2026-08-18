package com.helpdesk.api.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards domain events to RabbitMQ after the producing transaction has
 * committed (AFTER_COMMIT phase).
 *
 * Send failures are logged, never propagated: losing a confirmation-email
 * event must not fail the (already committed) ticket-creation request.
 * Known limitation: there is no outbox/retry, so a broker outage between
 * commit and publish drops the event. Acceptable until a delivery guarantee
 * is required.
 */
@Component
public class TicketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TicketEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public TicketEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitTopologyConfig.EXCHANGE_HELPDESK_EVENTS,
                    RabbitTopologyConfig.ROUTING_KEY_TICKET_CREATED,
                    event);
            log.info("Published ticket.created for ticket {} (requester={})",
                    event.ticketId(), event.requesterEmail());
        } catch (Exception ex) {
            log.error("Failed to publish ticket.created for ticket {}: {}",
                    event.ticketId(), ex.getMessage(), ex);
        }
    }
}
