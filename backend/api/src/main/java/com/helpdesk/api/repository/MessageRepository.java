package com.helpdesk.api.repository;

import com.helpdesk.api.domain.Message;
import com.helpdesk.api.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}