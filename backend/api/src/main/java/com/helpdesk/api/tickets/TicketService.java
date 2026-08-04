package com.helpdesk.api.tickets;

import com.helpdesk.api.domain.Category;
import com.helpdesk.api.domain.Message;
import com.helpdesk.api.domain.Role;
import com.helpdesk.api.domain.SenderType;
import com.helpdesk.api.domain.Ticket;
import com.helpdesk.api.domain.TicketPriority;
import com.helpdesk.api.domain.TicketStatus;
import com.helpdesk.api.domain.TicketType;
import com.helpdesk.api.domain.User;
import com.helpdesk.api.repository.CategoryRepository;
import com.helpdesk.api.repository.MessageRepository;
import com.helpdesk.api.repository.TicketRepository;
import com.helpdesk.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TicketService {

    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String ROLE_AGENT = "ROLE_AGENT";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = new EnumMap<>(TicketStatus.class);

    static {
        TRANSITIONS.put(TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS));
        TRANSITIONS.put(TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.OPEN));
        TRANSITIONS.put(TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS));
        TRANSITIONS.put(TicketStatus.CLOSED, Set.of(TicketStatus.OPEN));
    }

    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         MessageRepository messageRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest req, Authentication auth) {
        User author = auth != null ? requireUser(auth.getName()) : null;

        if (author == null && (req.requesterEmail() == null || req.requesterEmail().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "requesterEmail este obligatoriu pentru tichete anonime");
        }

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categorie inexistenta"));
        }

        Ticket ticket = Ticket.builder()
                .subject(req.subject())
                .description(req.description())
                .type(TicketType.FORM)
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.NORMAL)
                .category(category)
                .createdBy(author)
                .requesterEmail(req.requesterEmail())
                .build();
        ticketRepository.save(ticket);
        return TicketResponse.of(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketSummary> list(TicketStatus status,
                                     TicketPriority priority,
                                     TicketType type,
                                     Long categoryId,
                                     Long assignedToId,
                                     Authentication auth,
                                     Pageable pageable) {
        Long createdById = null;
        if (auth != null && hasRole(auth, ROLE_CLIENT)) {
            createdById = requireUser(auth.getName()).getId();
        }
        return ticketRepository.search(status, priority, type, categoryId, assignedToId, createdById, pageable)
                .map(TicketService::toSummary);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id, Authentication auth) {
        Ticket ticket = requireTicket(id);
        ensureCanView(ticket, auth);
        return TicketResponse.of(ticket);
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest req, Authentication auth) {
        requireStaff(auth);
        Ticket ticket = requireTicket(id);

        if (req.status() != null && req.status() != ticket.getStatus()) {
            assertTransition(ticket.getStatus(), req.status());
            ticket.setStatus(req.status());
            if (req.status() == TicketStatus.CLOSED) {
                ticket.setClosedAt(LocalDateTime.now());
            } else if (ticket.getClosedAt() != null) {
                ticket.setClosedAt(null);
            }
            if (req.status() == TicketStatus.OPEN) {
                ticket.setAssignedTo(null);
            }
        }

        if (req.priority() != null) {
            ticket.setPriority(req.priority());
        }

        if (req.assignedToId() != null) {
            User agent = userRepository.findById(req.assignedToId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent inexistent"));
            String role = "ROLE_" + agent.getRole().getName();
            if (!ROLE_AGENT.equals(role) && !ROLE_MANAGER.equals(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asignatul trebuie sa fie AGENT sau MANAGER");
            }
            ticket.setAssignedTo(agent);
            if (ticket.getStatus() == TicketStatus.OPEN) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            }
        }

        return TicketResponse.of(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> messages(Long id, Authentication auth) {
        Ticket ticket = requireTicket(id);
        ensureCanView(ticket, auth);
        return messageRepository.findByTicketOrderByCreatedAtAsc(ticket).stream()
                .map(MessageResponse::of)
                .toList();
    }

    @Transactional
    public MessageResponse addMessage(Long id, AddMessageRequest req, Authentication auth) {
        Ticket ticket = requireTicket(id);
        User sender = auth != null ? requireUser(auth.getName()) : null;
        ensureCanView(ticket, auth);

        SenderType senderType;
        if (sender == null) {
            senderType = SenderType.CLIENT;
        } else {
            senderType = hasRole(auth, ROLE_CLIENT) ? SenderType.CLIENT : SenderType.AGENT;
        }

        Message message = Message.builder()
                .ticket(ticket)
                .sender(sender)
                .senderType(senderType)
                .body(req.body())
                .build();
        messageRepository.save(message);
        return MessageResponse.of(message);
    }

    private Ticket requireTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tichet inexistent"));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizator inexistent"));
    }

    private void requireStaff(Authentication auth) {
        if (auth == null || (!hasRole(auth, ROLE_AGENT) && !hasRole(auth, ROLE_MANAGER))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces restrictionat la agenti");
        }
    }

    private void ensureCanView(Ticket ticket, Authentication auth) {
        if (auth == null || hasRole(auth, ROLE_CLIENT)) {
            User viewer = auth != null ? requireUser(auth.getName()) : null;
            boolean owner = viewer != null
                    && ticket.getCreatedBy() != null
                    && ticket.getCreatedBy().getId().equals(viewer.getId());
            boolean requesterMailMatch = viewer == null
                    && ticket.getRequesterEmail() != null;
            if (!owner && !requesterMailMatch) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces interzis la acest tichet");
            }
        }
    }

    private void assertTransition(TicketStatus from, TicketStatus to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tranzitie invalida: " + from + " -> " + to);
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    private static TicketSummary toSummary(Ticket t) {
        return new TicketSummary(
                t.getId(),
                t.getSubject(),
                t.getType(),
                t.getStatus(),
                t.getPriority(),
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getAssignedTo() != null ? t.getAssignedTo().getEmail() : null,
                t.getRequesterEmail(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}