package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findAllByTicket(SupportTicket ticket);

    List<TicketAttachment> findAllByTicketAndActiveTrueOrderByCreatedAtAsc(SupportTicket ticket);

    /** Pour une page de tickets · ticket, pieces jointes, pieces visibles du client. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT a.ticket.id, COUNT(a), SUM(CASE WHEN a.internal = true THEN 0 ELSE 1 END)
            FROM TicketAttachment a
            WHERE a.active = true AND a.ticket.id IN :ticketIds
            GROUP BY a.ticket.id
            """)
    List<Object[]> countActiveByTickets(@org.springframework.data.repository.query.Param("ticketIds") java.util.Collection<Long> ticketIds);

    Optional<TicketAttachment> findByIdAndTicket(Long id, SupportTicket ticket);
}
