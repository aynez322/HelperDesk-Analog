package com.helpdesk.api.tickets;

import com.helpdesk.api.domain.*;
import com.helpdesk.api.events.TicketCreatedEvent;
import com.helpdesk.api.repository.CategoryRepository;
import com.helpdesk.api.repository.MessageRepository;
import com.helpdesk.api.repository.TicketRepository;
import com.helpdesk.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock MessageRepository messageRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    TicketService service;

    User client;
    User agent;
    User manager;

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, messageRepository, categoryRepository,
                userRepository, eventPublisher);
        client = user(1L, "client@helpdesk.local", "CLIENT");
        agent = user(2L, "agent@helpdesk.local", "AGENT");
        manager = user(3L, "manager@helpdesk.local", "MANAGER");
    }

    private User user(Long id, String email, String role) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName("User " + role)
                .role(Role.builder().name(role).build())
                .active(true)
                .build();
    }

    private Ticket ticket(Long id, User createdBy, TicketStatus status, TicketPriority priority) {
        return Ticket.builder()
                .id(id)
                .subject("Subject")
                .description("Description")
                .type(TicketType.FORM)
                .status(status)
                .priority(priority)
                .createdBy(createdBy)
                .build();
    }

    private Authentication auth(User user, String role) {
        return new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    // ---------- create() ----------

    @Test
    void create_anonymousWithoutEmail_throws400() {
        CreateTicketRequest req = new CreateTicketRequest("Subject", "Desc", null, null);
        assertThatThrownBy(() -> service.create(req, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void create_anonymousWithEmail_createsFormTicketAndPublishesEvent() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });
        CreateTicketRequest req = new CreateTicketRequest("Subject", "Desc", null, "anon@example.com");

        TicketResponse response = service.create(req, null);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.type()).isEqualTo(TicketType.FORM);
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.priority()).isEqualTo(TicketPriority.NORMAL);
        assertThat(response.requesterEmail()).isEqualTo("anon@example.com");
        assertThat(response.createdById()).isNull();

        ArgumentCaptor<TicketCreatedEvent> captor = ArgumentCaptor.forClass(TicketCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().requesterEmail()).isEqualTo("anon@example.com");
        assertThat(captor.getValue().ticketId()).isEqualTo(42L);
    }

    @Test
    void create_authenticatedClient_usesAuthorEmailAsNotifyTarget() {
        when(userRepository.findByEmail(client.getEmail())).thenReturn(Optional.of(client));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });
        CreateTicketRequest req = new CreateTicketRequest("Subject", "Desc", null, null);

        TicketResponse response = service.create(req, auth(client, "CLIENT"));

        assertThat(response.createdById()).isEqualTo(1L);
        ArgumentCaptor<TicketCreatedEvent> captor = ArgumentCaptor.forClass(TicketCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().requesterEmail()).isEqualTo(client.getEmail());
    }

    @Test
    void create_withUnknownCategory_throws404() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        CreateTicketRequest req = new CreateTicketRequest("Subject", "Desc", 99L, "a@example.com");
        assertThatThrownBy(() -> service.create(req, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- update() / state machine ----------

    @Test
    void update_nonStaff_throws403() {
        UpdateTicketRequest req = new UpdateTicketRequest(TicketStatus.IN_PROGRESS, null, null);
        assertThatThrownBy(() -> service.update(1L, req, auth(client, "CLIENT")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void update_illegalTransition_throws409() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket(1L, client, TicketStatus.OPEN, TicketPriority.NORMAL)));
        // OPEN -> RESOLVED is illegal (must go through IN_PROGRESS)
        UpdateTicketRequest req = new UpdateTicketRequest(TicketStatus.RESOLVED, null, null);
        assertThatThrownBy(() -> service.update(1L, req, auth(agent, "AGENT")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void update_validTransition_persists() {
        Ticket t = ticket(1L, client, TicketStatus.OPEN, TicketPriority.NORMAL);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTicketRequest req = new UpdateTicketRequest(TicketStatus.IN_PROGRESS, null, null);
        TicketResponse response = service.update(1L, req, auth(agent, "AGENT"));

        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(ticketRepository).save(t);
    }

    @Test
    void update_assignAgent_promotesOpenToInProgress() {
        Ticket t = ticket(1L, client, TicketStatus.OPEN, TicketPriority.NORMAL);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(userRepository.findById(2L)).thenReturn(Optional.of(agent));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTicketRequest req = new UpdateTicketRequest(null, null, 2L);
        TicketResponse response = service.update(1L, req, auth(agent, "AGENT"));

        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(response.assignedToEmail()).isEqualTo(agent.getEmail());
    }

    @Test
    void update_assignClient_throws400() {
        Ticket t = ticket(1L, client, TicketStatus.OPEN, TicketPriority.NORMAL);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));

        UpdateTicketRequest req = new UpdateTicketRequest(null, null, 1L);
        assertThatThrownBy(() -> service.update(1L, req, auth(agent, "AGENT")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_close_setsClosedAt() {
        Ticket t = ticket(1L, client, TicketStatus.RESOLVED, TicketPriority.NORMAL);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTicketRequest req = new UpdateTicketRequest(TicketStatus.CLOSED, null, null);
        TicketResponse response = service.update(1L, req, auth(manager, "MANAGER"));

        assertThat(response.status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(response.closedAt()).isNotNull();
    }

    @Test
    void update_reopen_clearsAssignedToAndClosedAt() {
        Ticket t = ticket(1L, client, TicketStatus.CLOSED, TicketPriority.NORMAL);
        t.setAssignedTo(agent);
        t.setClosedAt(java.time.LocalDateTime.now());
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTicketRequest req = new UpdateTicketRequest(TicketStatus.OPEN, null, null);
        TicketResponse response = service.update(1L, req, auth(manager, "MANAGER"));

        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.assignedToId()).isNull();
        assertThat(response.closedAt()).isNull();
    }

    // ---------- get() / ownership ----------

    @Test
    void get_ownerClient_canView() {
        Ticket t = ticket(1L, client, TicketStatus.OPEN, TicketPriority.NORMAL);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(userRepository.findByEmail(client.getEmail())).thenReturn(Optional.of(client));

        TicketResponse response = service.get(1L, auth(client, "CLIENT"));

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void get_otherClient_forbidden() {
        User other = user(9L, "other@helpdesk.local", "CLIENT");
        Ticket t = ticket(1L, client, TicketStatus.OPEN, TicketPriority.NORMAL);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.get(1L, auth(other, "CLIENT")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void get_staff_canViewAny() {
        Ticket t = ticket(1L, client, TicketStatus.OPEN, TicketPriority.NORMAL);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));

        TicketResponse response = service.get(1L, auth(agent, "AGENT"));

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void get_unknownTicket_throws404() {
        when(ticketRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(404L, auth(agent, "AGENT")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- addMessage() ----------

    @Test
    void addMessage_anonymousClient_senderTypeClient() {
        Ticket t = ticket(1L, null, TicketStatus.OPEN, TicketPriority.NORMAL);
        t.setRequesterEmail("anon@example.com");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(5L);
            return m;
        });

        MessageResponse response = service.addMessage(1L, "Hello anon", null);

        assertThat(response.senderType()).isEqualTo(SenderType.CLIENT);
        assertThat(response.body()).isEqualTo("Hello anon");
    }

    @Test
    void addMessage_agentSender_senderTypeAgent() {
        Ticket t = ticket(1L, client, TicketStatus.IN_PROGRESS, TicketPriority.NORMAL);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(userRepository.findByEmail(agent.getEmail())).thenReturn(Optional.of(agent));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(6L);
            return m;
        });

        MessageResponse response = service.addMessage(1L, "Reply from agent", auth(agent, "AGENT"));

        assertThat(response.senderType()).isEqualTo(SenderType.AGENT);
        assertThat(response.senderEmail()).isEqualTo(agent.getEmail());
    }

    // ---------- createLiveTicket() ----------

    @Test
    void createLiveTicket_anonymousWithoutEmail_throws400() {
        assertThatThrownBy(() -> service.createLiveTicket("Hi", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createLiveTicket_mintsUrgentLiveTicket() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        Ticket t = service.createLiveTicket("Salut, am o problema", "anon@example.com", null);

        assertThat(t.getId()).isEqualTo(10L);
        assertThat(t.getType()).isEqualTo(TicketType.LIVE);
        assertThat(t.getPriority()).isEqualTo(TicketPriority.URGENT);
        assertThat(t.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(t.getRequesterEmail()).isEqualTo("anon@example.com");
    }

    @Test
    void createLiveTicket_longMessage_truncatesSubject() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        String longMsg = "a".repeat(200);
        Ticket t = service.createLiveTicket(longMsg, "anon@example.com", null);
        assertThat(t.getSubject()).hasSize(63); // 60 + "..."
        assertThat(t.getSubject()).endsWith("...");
    }

    // ---------- list() / client auto-scoping ----------

    @Test
    void list_clientOnlySeesOwnTickets() {
        when(userRepository.findByEmail(client.getEmail())).thenReturn(Optional.of(client));
        when(ticketRepository.search(any(), any(), any(), any(), any(), eq(1L), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.list(null, null, null, null, null, auth(client, "CLIENT"),
                org.springframework.data.domain.PageRequest.of(0, 20));

        verify(ticketRepository).search(any(), any(), any(), any(), any(), eq(1L), any());
    }

    @Test
    void list_staffSeesAllTickets() {
        when(ticketRepository.search(any(), any(), any(), any(), any(), isNull(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.list(null, null, null, null, null, auth(agent, "AGENT"),
                org.springframework.data.domain.PageRequest.of(0, 20));

        verify(ticketRepository).search(any(), any(), any(), any(), any(), isNull(), any());
    }
}
