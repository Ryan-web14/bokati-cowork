package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Piece comptable. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_entry")
public class AccEntry {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "company_code", nullable = false, length = 40)
    private String companyCode = "MAIN";

    @Column(name = "entry_number", unique = true, length = 60)
    private String entryNumber;

    @Column(name = "provisional_number", nullable = false, unique = true, length = 60)
    private String provisionalNumber;

    @Column(name = "journal_code", nullable = false, length = 30)
    private String journalCode;

    @Column(name = "fiscal_year_code", nullable = false, length = 10)
    private String fiscalYearCode;

    @Column(name = "period_code", nullable = false, length = 10)
    private String periodCode;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "original_date")
    private LocalDate originalDate;

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Column(name = "label", nullable = false, length = 500)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.EntryOrigin origin;

    @Column(name = "source_type", length = 60)
    private String sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.EntryStatus status;

    @Builder.Default
    @Column(name = "accepted_unbalanced", nullable = false)
    private Boolean acceptedUnbalanced = Boolean.FALSE;

    @Column(name = "unbalanced_amount", precision = 19, scale = 4)
    private BigDecimal unbalancedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_reason", length = 60)
    private com.sni.bokaticowork.features.accounting.enums.PendingReason pendingReason;

    @Column(name = "pending_detail", length = 1000)
    private String pendingDetail;

    @Builder.Default
    @Column(name = "total_debit", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_credit", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "created_by", nullable = false, length = 180)
    private String createdBy;

    @Column(name = "validated_by", length = 180)
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "reversal_of", length = 60)
    private String reversalOf;

    @Column(name = "reversed_by_entry", length = 60)
    private String reversedByEntry;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    @Column(name = "regularization_of", length = 60)
    private String regularizationOf;

    @Column(name = "regularized_by", length = 60)
    private String regularizedBy;

    @Column(name = "rejected_by", length = 180)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "attachments_json", columnDefinition = "jsonb")
    private String attachmentsJson;

    /** Evenement comptable qui a produit la piece (lot F2). */
    @Column(name = "posting_event_id")
    private Long postingEventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
