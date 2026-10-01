package com.sni.bokaticowork.features.subscription.lifecycle.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un gel · compte, borne et facture selon la politique.
 *
 * <p>Un gel sans bornes est un moyen de ne plus payer sans resilier. Chaque gel est donc ecrit,
 * avec ses jours prevus et effectifs, pour que la politique puisse compter.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_freeze")
public class SubscriptionFreeze {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    @Column(name = "planned_until", nullable = false)
    private LocalDate plannedUntil;

    @Column(name = "resumed_on")
    private LocalDate resumedOn;

    @Column(name = "days_planned", nullable = false)
    private Integer daysPlanned;

    @Column(name = "days_effective")
    private Integer daysEffective;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "fee_billable_number", length = 100)
    private String feeBillableNumber;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
