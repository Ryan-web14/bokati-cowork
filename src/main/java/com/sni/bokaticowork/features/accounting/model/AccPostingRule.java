package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.accounting.enums.PostingRuleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Schema d'ecriture (lot F2) : pour un type d'evenement et des conditions, le journal et les lignes
 * a produire. Les variables entre accolades sont resolues a l'execution (voir {@code PostingTemplate}).
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_posting_rule")
public class AccPostingRule {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "family", nullable = false, length = 40)
    private String family;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    /** Objet JSON : champ de l'evenement vers valeur attendue (ou liste de valeurs). */
    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions;

    /** Code du journal, ou variable ({@code {journalCode}}). */
    @Column(name = "journal", length = 80)
    private String journal;

    @Column(name = "entry_label", length = 500)
    private String entryLabel;

    /** Tableau JSON des lignes (voir {@code PostingTemplate.LineSpec}). */
    @Column(name = "lines", columnDefinition = "TEXT")
    private String lines;

    /** Le fait ne produit pas d'ecriture : l'evenement est ecarte. */
    @Builder.Default
    @Column(name = "no_entry", nullable = false)
    private Boolean noEntry = Boolean.FALSE;

    @Column(name = "sample_payload", columnDefinition = "TEXT")
    private String samplePayload;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private PostingRuleStatus status = PostingRuleStatus.DRAFT;

    @Column(name = "activated_by", length = 180)
    private String activatedBy;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "created_by", nullable = false, length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (effectiveFrom == null) {
            effectiveFrom = LocalDate.of(2026, 1, 1);
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    /** Code de flux du tableau des flux de tresorerie (FF a FQ) des pieces de ce schema (complements F12). */
    @Column(name = "cash_flow_code", length = 4)
    private String cashFlowCode;
}
