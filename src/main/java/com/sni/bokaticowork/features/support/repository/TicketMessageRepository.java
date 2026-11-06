package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findAllByTicketOrderByCreatedAtAsc(SupportTicket ticket);

    /**
     * Le fil d'un ticket · un message plus long que {@code maxLength} revient sans son contenu, pour
     * qu'un seul message demesure ne puisse pas remplir la memoire.
     */
    @Query("""
            SELECT new com.sni.bokaticowork.features.support.repository.TicketMessageRepository$MessageView(
                m.id, m.senderType, m.senderId, m.senderName,
                CASE WHEN LENGTH(m.message) > :maxLength THEN NULL ELSE m.message END,
                LENGTH(m.message), m.internal, m.createdAt)
            FROM TicketMessage m
            WHERE m.ticket.id = :ticketId
            ORDER BY m.createdAt ASC
            """)
    List<MessageView> findViewsByTicket(@Param("ticketId") Long ticketId, @Param("maxLength") int maxLength);

    /** Pour une page de tickets · ticket, messages, messages visibles du client. */
    @Query("""
            SELECT m.ticket.id, COUNT(m), SUM(CASE WHEN m.internal = true THEN 0 ELSE 1 END)
            FROM TicketMessage m
            WHERE m.ticket.id IN :ticketIds
            GROUP BY m.ticket.id
            """)
    List<Object[]> countByTickets(@Param("ticketIds") Collection<Long> ticketIds);

    record MessageView(Long id, TicketSenderType senderType, String senderId, String senderName, String content,
                       Integer contentLength, Boolean internal, Instant createdAt) {
    }
    boolean existsByExternalMessageId(String externalMessageId);
}
