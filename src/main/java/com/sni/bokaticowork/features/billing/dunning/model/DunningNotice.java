package com.sni.bokaticowork.features.billing.dunning.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Une relance executee · par facture et par palier, une seule fois. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "dunning_notice")
public class DunningNotice {

    public enum Outcome {
        SENT, SKIPPED, FAILED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private DunningStep step;

    @Column(name = "policy_code", nullable = false, length = 40)
    private String policyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private DunningStep.Action action;

    @Column(name = "customer_type", nullable = false, length = 40)
    private String customerType;

    @Column(name = "customer_code", nullable = false, length = 120)
    private String customerCode;

    @Column(name = "subscription_number", length = 100)
    private String subscriptionNumber;

    @Column(name = "days_overdue", nullable = false)
    private Integer daysOverdue;

    @Column(name = "balance_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private Outcome outcome;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @PrePersist
    public void prePersist() {
        if (executedAt == null) {
            executedAt = Instant.now();
        }
    }
}
