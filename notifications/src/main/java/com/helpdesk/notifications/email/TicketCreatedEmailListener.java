package com.helpdesk.notifications.email;

import com.helpdesk.notifications.config.RabbitConsumerConfig;
import com.helpdesk.notifications.events.TicketCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Consumes {@code ticket.created} events from {@code notifications.email} and
 * sends a confirmation email to the requester (Romanian copy, per project
 * convention for user-facing strings).
 *
 * Delivery semantics: with in-memory retry (3 attempts) configured and
 * {@code default-requeue-rejected: false}, an email that keeps failing after
 * retries is dropped with an error log — no requeue hot loop. There is no DLQ
 * yet; add one before relying on this in production.
 */
@Component
public class TicketCreatedEmailListener {

    private static final Logger log = LoggerFactory.getLogger(TicketCreatedEmailListener.class);

    private final JavaMailSender mailSender;
    private final String from;

    public TicketCreatedEmailListener(JavaMailSender mailSender,
                                       @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @RabbitListener(queues = RabbitConsumerConfig.QUEUE_NOTIFICATIONS_EMAIL)
    public void onTicketCreated(TicketCreatedEvent event) {
        if (!StringUtils.hasText(event.requesterEmail())) {
            log.warn("ticket.created for ticket {} has no requesterEmail — skipping notification",
                    event.ticketId());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(event.requesterEmail());
        mail.setSubject("[HelpDesk] Tichetul #" + event.ticketId() + " a fost inregistrat");
        mail.setText(body(event));
        mailSender.send(mail);

        log.info("Confirmation email sent for ticket {} to {}", event.ticketId(), event.requesterEmail());
    }

    private String body(TicketCreatedEvent e) {
        return """
                Buna ziua,

                Solicitarea dvs. de suport a fost inregistrata cu succes.

                Detalii tichet:
                - Numar: #%d
                - Subiect: %s
                - Categorie: %s
                - Prioritate: %s

                Un agent va analiza solicitarea si va va raspunde in cel mai scurt timp.

                Va multumim,
                Echipa HelpDesk
                """.formatted(
                e.ticketId(),
                e.subject(),
                e.category() != null ? e.category() : "Necategorizata",
                priorityLabel(e.priority()));
    }

    private String priorityLabel(String priority) {
        if (priority == null) return "normala";
        return switch (priority) {
            case "LOW" -> "scazuta";
            case "HIGH" -> "ridicata";
            case "URGENT" -> "urgenta";
            default -> "normala";
        };
    }
}
