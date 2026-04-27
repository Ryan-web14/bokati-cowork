package com.sni.bokaticowork.features.subscription.change.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeEffectivePolicy;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeStatus;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_change_request", indexes = {
        @Index(name = "idx_subscription_change_subscription", columnList = "subscription_id,status"),
        @Index(name = "idx_subscription_change_effective", columnList = "effective_policy,effective_date,status")
})
public class SubscriptionChangeRequest {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "change_number", nullable = false, unique = true, length = 100)
    private String changeNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_change_subscription"))
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 60)
    private SubscriptionChangeType changeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_plan_version_id", foreignKey = @ForeignKey(name = "fk_subscription_change_current_plan"))
    private PlanVersion currentPlanVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_plan_version_id", foreignKey = @ForeignKey(name = "fk_subscription_change_target_plan"))
    private PlanVersion targetPlanVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "effective_policy", nullable = false, length = 60)
    private SubscriptionChangeEffectivePolicy effectivePolicy;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "proration_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal prorationAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private SubscriptionChangeStatus status = SubscriptionChangeStatus.REQUESTED;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
