package com.sni.bokaticowork.core.communication.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    List<WhatsAppMessage> findTop200ByOrderByCreatedAtDesc();

    List<WhatsAppMessage> findAllByReferenceOrderByCreatedAtDesc(String reference);
}
