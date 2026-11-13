package com.sni.bokaticowork.core.communication.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemplate, Long> {
    List<WhatsAppTemplate> findAllByOrderByPurposeAscNameAsc();

    Optional<WhatsAppTemplate> findFirstByPurposeAndLanguageAndStatusOrderByCreatedAtDesc(String purpose, String language, String status);

    Optional<WhatsAppTemplate> findByNameAndLanguage(String name, String language);

    List<WhatsAppTemplate> findAllByPurpose(String purpose);
}
