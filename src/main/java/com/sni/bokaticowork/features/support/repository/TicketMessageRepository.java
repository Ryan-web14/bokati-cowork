package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findAllByTicketOrderByCreatedAtAsc(SupportTicket ticket);
}
