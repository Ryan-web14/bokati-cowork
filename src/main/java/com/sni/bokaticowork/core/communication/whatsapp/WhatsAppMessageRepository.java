package com.sni.bokaticowork.core.communication.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {
    List<WhatsAppMessage> findTop200ByOrderByCreatedAtDesc();

    List<WhatsAppMessage> findAllByReferenceOrderByCreatedAtDesc(String reference);

    java.util.Optional<WhatsAppMessage> findByMessageId(String messageId);

    /** Messages pris en charge mais pas encore remis ni en echec, envoyes depuis la date. */
    @org.springframework.data.jpa.repository.Query("SELECT m FROM WhatsAppMessage m WHERE m.createdAt >= :since AND m.error IS NULL "
            + "AND (m.statusGroup IS NULL OR m.statusGroup = 'PENDING') ORDER BY m.createdAt ASC")
    List<WhatsAppMessage> findPending(@org.springframework.data.repository.query.Param("since") java.time.Instant since,
                                      org.springframework.data.domain.Pageable page);
}
