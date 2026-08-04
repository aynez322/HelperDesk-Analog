package com.helpdesk.api.tickets;

import com.helpdesk.api.domain.TicketPriority;
import com.helpdesk.api.domain.TicketStatus;
import com.helpdesk.api.domain.TicketType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest req,
                                                  Authentication authentication) {
        TicketResponse created = ticketService.create(req, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<TicketSummary> list(@RequestParam(required = false) TicketStatus status,
                                     @RequestParam(required = false) TicketPriority priority,
                                     @RequestParam(required = false) TicketType type,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) Long assignedToId,
                                     Authentication authentication,
                                     @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ticketService.list(status, priority, type, categoryId, assignedToId, authentication, pageable);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, Authentication authentication) {
        return ticketService.get(id, authentication);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id,
                                @RequestBody UpdateTicketRequest req,
                                Authentication authentication) {
        return ticketService.update(id, req, authentication);
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id, Authentication authentication) {
        return ticketService.messages(id, authentication);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> addMessage(@PathVariable Long id,
                                                       @Valid @RequestBody AddMessageRequest req,
                                                       Authentication authentication) {
        MessageResponse created = ticketService.addMessage(id, req, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}