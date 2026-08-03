package com.helpdesk.api.repository;

import com.helpdesk.api.domain.Ticket;
import com.helpdesk.api.domain.TicketPriority;
import com.helpdesk.api.domain.TicketStatus;
import com.helpdesk.api.domain.TicketType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId)
              AND (:createdById IS NULL OR t.createdBy.id = :createdById)
            """)
    Page<Ticket> search(@Param("status") TicketStatus status,
                       @Param("priority") TicketPriority priority,
                       @Param("type") TicketType type,
                       @Param("categoryId") Long categoryId,
                       @Param("assignedToId") Long assignedToId,
                       @Param("createdById") Long createdById,
                       Pageable pageable);
}