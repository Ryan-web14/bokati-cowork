package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_plan_price", indexes = {
        @Index(name = "idx_plan_price_version", columnList = "plan_version_id")
})
public class PlanPrice {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_version_id", nullable = false, foreignKey = @ForeignKey(name = "fk_plan_price_version"))
    private PlanVersion planVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 40)
    private BillingCycle billingCycle;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "setup_fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal setupFee = BigDecimal.ZERO;

    @Column(name = "deposit_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "tax_included", nullable = false)
    @Builder.Default
    private Boolean taxIncluded = Boolean.TRUE;

    @Column(name = "trial_days", nullable = false)
    @Builder.Default
    private Integer trialDays = 0;

    @Column(name = "commitment_months", nullable = false)
    @Builder.Default
    private Integer commitmentMonths = 0;

    @Column(name = "tax_code", length = 80)
    private String taxCode;

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
