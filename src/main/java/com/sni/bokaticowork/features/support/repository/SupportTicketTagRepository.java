package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.model.SupportTag;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.SupportTicketTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SupportTicketTagRepository extends JpaRepository<SupportTicketTag, Long> {
    List<SupportTicketTag> findAllByTicketOrderByCreatedAtAsc(SupportTicket ticket);

    Optional<SupportTicketTag> findByTicketAndTag(SupportTicket ticket, SupportTag tag);

    boolean existsByTicketAndTag(SupportTicket ticket, SupportTag tag);

    @Query("""
            SELECT tag.name, COUNT(stt)
            FROM SupportTicketTag stt
            JOIN stt.tag tag
            JOIN stt.ticket tk
            WHERE tk.createdAt >= :from AND tk.createdAt <= :to
            GROUP BY tag.name
            ORDER BY COUNT(stt) DESC
            """)
    List<Object[]> topTagsBetween(@Param("from") Instant from, @Param("to") Instant to);
}
