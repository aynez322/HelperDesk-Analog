package com.helpdesk.notifications.email;

import com.helpdesk.notifications.config.RabbitConsumerConfig;
import com.helpdesk.notifications.events.TicketCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCreatedEmailListenerTest {

    @Mock JavaMailSender mailSender;

    TicketCreatedEmailListener listener;

    @BeforeEach
    void setUp() {
        listener = new TicketCreatedEmailListener(mailSender, "helpdesk@helpdesk.local");
    }

    private TicketCreatedEvent event(String email, Long id, String category, String priority) {
        return new TicketCreatedEvent(id, "Tichet test", "Descriere", email, "FORM",
                priority, category, LocalDateTime.of(2026, 8, 20, 10, 0));
    }

    @Test
    void sendsRomanianConfirmationEmail() {
        listener.onTicketCreated(event("client@example.com", 5L, "Facturare", "NORMAL"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage m = captor.getValue();

        assertThat(m.getFrom()).isEqualTo("helpdesk@helpdesk.local");
        assertThat(m.getTo()).containsExactly("client@example.com");
        assertThat(m.getSubject()).isEqualTo("[HelpDesk] Tichetul #5 a fost inregistrat");
        assertThat(m.getText())
                .contains("Tichet test")
                .contains("Facturare")
                .contains("normala");
    }

    @Test
    void mapsUrgentPriorityToRomanianLabel() {
        listener.onTicketCreated(event("client@example.com", 6L, null, "URGENT"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("urgenta");
        assertThat(captor.getValue().getText()).contains("Necategorizata");
    }

    @Test
    void skipsWhenNoRequesterEmail() {
        listener.onTicketCreated(event(null, 7L, null, "NORMAL"));

        verifyNoInteractions(mailSender);
    }

    @Test
    void skipsWhenRequesterEmailBlank() {
        listener.onTicketCreated(event("   ", 8L, null, "NORMAL"));

        verifyNoInteractions(mailSender);
    }
}
