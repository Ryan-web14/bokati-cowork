package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pointage : lignes de releve et lignes d'ecriture de meme total. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_matching")
public class BankMatching {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "bank_account_code", nullable = false)
    private String bankAccountCode;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "statement_line_ids", nullable = false, columnDefinition = "TEXT")
    private String statementLineIds;

    @Column(name = "entry_line_ids", nullable = false, columnDefinition = "TEXT")
    private String entryLineIds;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Builder.Default
    @Column(name = "locked", nullable = false)
    private Boolean locked = Boolean.FALSE;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @Column(name = "unmatched_by")
    private String unmatchedBy;

    @Column(name = "unmatched_at")
    private java.time.Instant unmatchedAt;

    @Column(name = "unmatched_reason")
    private String unmatchedReason;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
