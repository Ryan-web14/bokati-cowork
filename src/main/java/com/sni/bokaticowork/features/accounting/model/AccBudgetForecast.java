package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Prevision de depense : quoi et quand. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget_forecast")
public class AccBudgetForecast {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "section_code")
    private String sectionCode;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "planned_month", nullable = false)
    private String plannedMonth;

    @Builder.Default
    @Column(name = "recurrence", nullable = false)
    private String recurrence = "NONE";

    @Builder.Default
    @Column(name = "occurrences", nullable = false)
    private Integer occurrences = 1;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "responsible")
    private String responsible;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private String priority = "IMPORTANT";

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "PLANNED";

    @Builder.Default
    @Column(name = "realized_amount", nullable = false)
    private java.math.BigDecimal realizedAmount = java.math.BigDecimal.ZERO;

    @Column(name = "reminded_at")
    private java.time.Instant remindedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
