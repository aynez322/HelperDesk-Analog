package com.helpdesk.notifications.email;

import com.helpdesk.notifications.config.RabbitConsumerConfig;
import com.helpdesk.notifications.events.TicketReplyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TicketReplyEmailListener {

    private static final Logger log = LoggerFactory.getLogger(TicketReplyEmailListener.class);

    private final JavaMailSender mailSender;
    private final String from;

    public TicketReplyEmailListener(JavaMailSender mailSender,
                                    @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @RabbitListener(queues = RabbitConsumerConfig.QUEUE_NOTIFICATIONS_EMAIL_REPLY)
    public void onTicketReplied(TicketReplyEvent event) {
        if (!StringUtils.hasText(event.requesterEmail())) {
            log.warn("ticket.replied for ticket {} has no requesterEmail — skipping notification",
                    event.ticketId());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(event.requesterEmail());
        mail.setSubject("[HelpDesk] Ai primit un raspuns la tichetul #" + event.ticketId());
        mail.setText(body(event));
        mailSender.send(mail);

        log.info("Reply email sent for ticket {} to {}", event.ticketId(), event.requesterEmail());
    }

    private String body(TicketReplyEvent e) {
        return """
                Buna ziua,

                Un agent de suport a raspuns la solicitarea dvs. de pe platforma HelpDesk.

                Detalii tichet:
                - Numar: #%d
                - Subiect: %s

                Pentru a citi raspunsul si a continua conversatia, va rugam sa va
                conectati pe platforma si sa deschideti tichetul.

                Va multumim,
                Echipa HelpDesk
                """.formatted(
                e.ticketId(),
                e.subject() != null ? e.subject() : "-");
    }
}
