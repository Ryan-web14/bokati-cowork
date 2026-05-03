package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);
    Page<SupportTicket> findAllByStatus(TicketStatus status, Pageable pageable);
    Page<SupportTicket> findAllByOwnerTypeAndOwnerCode(String ownerType, String ownerCode, Pageable pageable);

    @Query("""
            select ticket
            from SupportTicket ticket
            where (:status is null or ticket.status = :status)
              and (:assignedTo is null or ticket.assignedTo = :assignedTo)
              and (:ownerType is null or ticket.ownerType = :ownerType)
              and (:ownerCode is null or ticket.ownerCode = :ownerCode)
            """)
    Page<SupportTicket> search(@Param("status") TicketStatus status,
                               @Param("assignedTo") Long assignedTo,
                               @Param("ownerType") String ownerType,
                               @Param("ownerCode") String ownerCode,
                               Pageable pageable);
}
