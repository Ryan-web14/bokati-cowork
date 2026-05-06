package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.model.QuickReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuickReplyRepository extends JpaRepository<QuickReply, Long> {
    List<QuickReply> findAllByActiveTrue();
    List<QuickReply> findAllByCategoryAndActiveTrue(TicketCategory category);
}
