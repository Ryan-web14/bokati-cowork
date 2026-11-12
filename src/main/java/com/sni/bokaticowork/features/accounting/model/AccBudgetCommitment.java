package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Engagement sur une ligne de budget. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget_commitment")
public class AccBudgetCommitment {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "reference", nullable = false)
    private String reference;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Builder.Default
    @Column(name = "settled_amount", nullable = false)
    private java.math.BigDecimal settledAmount = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "OPEN";

    @Column(name = "forecast_number")
    private String forecastNumber;

    @Builder.Default
    @Column(name = "unplanned", nullable = false)
    private Boolean unplanned = Boolean.FALSE;

    @Column(name = "unplanned_code")
    private String unplannedCode;

    @Column(name = "unplanned_text")
    private String unplannedText;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @Column(name = "closed_at")
    private java.time.Instant closedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
