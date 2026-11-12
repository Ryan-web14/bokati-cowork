package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Derogation au blocage budgetaire, tracee. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget_override")
public class AccBudgetOverride {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "reference", nullable = false)
    private String reference;

    @Column(name = "overrun", nullable = false)
    private java.math.BigDecimal overrun;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "granted_by", nullable = false)
    private String grantedBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
