package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findAllByTicket(SupportTicket ticket);

    List<TicketAttachment> findAllByTicketAndActiveTrueOrderByCreatedAtAsc(SupportTicket ticket);

    Optional<TicketAttachment> findByIdAndTicket(Long id, SupportTicket ticket);
}
