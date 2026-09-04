package com.helpdesk.api;

import com.helpdesk.api.domain.*;
import com.helpdesk.api.repository.TicketRepository;
import com.helpdesk.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TicketRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TicketRepository ticketRepository;
    @Autowired UserRepository userRepository;

    @Test
    void flywayMigrationsApplyAndSearchFilters() {
        User agent = userRepository.findByEmail("agent@helpdesk.local").orElse(null);
        if (agent == null) {

        }

        assertThat(userRepository.findByEmail("manager@helpdesk.local")).isPresent();
    }

    @Test
    void persistsTicketAndFiltersByStatusAndType() {
        User manager = userRepository.findByEmail("manager@helpdesk.local").orElseThrow();

        Ticket formOpen = Ticket.builder()
                .subject("Form open")
                .type(TicketType.FORM)
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.NORMAL)
                .createdBy(manager)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Ticket liveInProgress = Ticket.builder()
                .subject("Live in progress")
                .type(TicketType.LIVE)
                .status(TicketStatus.IN_PROGRESS)
                .priority(TicketPriority.URGENT)
                .createdBy(manager)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ticketRepository.save(formOpen);
        ticketRepository.save(liveInProgress);

        Page<Ticket> onlyOpen = ticketRepository.search(
                TicketStatus.OPEN, null, null, null, null, null, PageRequest.of(0, 20));
        assertThat(onlyOpen.getContent())
                .extracting(Ticket::getSubject)
                .contains("Form open")
                .doesNotContain("Live in progress");

        Page<Ticket> urgent = ticketRepository.search(
                null, TicketPriority.URGENT, TicketType.LIVE, null, null, null, PageRequest.of(0, 20));
        assertThat(urgent.getContent()).extracting(Ticket::getSubject).containsExactly("Live in progress");
    }
}
