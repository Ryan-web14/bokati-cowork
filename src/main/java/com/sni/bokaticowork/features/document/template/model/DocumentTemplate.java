package com.sni.bokaticowork.features.document.template.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Modele de document (lot D2) : une piece imprimee ou un courriel, designe par un code stable
 * ({@code CASH_OUT_VOUCHER}, {@code FUND_REQUEST}...). Le contenu vit dans ses versions ; sans
 * version publiee, la piece sort du gabarit Thymeleaf d'origine ({@code fallbackTemplate}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_template")
public class DocumentTemplate {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 80, unique = true)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    /** {@code PDF} ou {@code EMAIL}. */
    @Column(name = "family", nullable = false, length = 20)
    private String family;

    /** Domaine metier : CASH, FUND, ACCOUNTING, BILLING, PAYMENT, BOOKING... */
    @Column(name = "domain", nullable = false, length = 40)
    private String domain;

    @Column(name = "fallback_template", length = 150)
    private String fallbackTemplate;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
