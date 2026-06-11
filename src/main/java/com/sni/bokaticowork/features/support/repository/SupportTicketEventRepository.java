package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.enums.TicketEventType;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.SupportTicketEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SupportTicketEventRepository extends JpaRepository<SupportTicketEvent, Long> {
    List<SupportTicketEvent> findAllByTicketOrderByCreatedAtAsc(SupportTicket ticket);

    @Query("""
            SELECT COUNT(e) FROM SupportTicketEvent e
            WHERE e.eventType = :eventType
              AND e.createdAt >= :from AND e.createdAt <= :to
            """)
    long countByEventTypeBetween(@Param("eventType") TicketEventType eventType,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);
}
