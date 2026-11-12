package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Virement de credits entre lignes. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget_transfer")
public class AccBudgetTransfer {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "from_line_id", nullable = false)
    private Long fromLineId;

    @Column(name = "to_line_id", nullable = false)
    private Long toLineId;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "REQUESTED";

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private java.time.Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
