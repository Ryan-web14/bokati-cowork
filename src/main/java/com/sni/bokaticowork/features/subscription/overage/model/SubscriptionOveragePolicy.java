package com.sni.bokaticowork.features.subscription.overage.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.overage.enums.OveragePolicyMode;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import jakarta.persistence.*;
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
@Table(name = "subscription_overage_policy", indexes = {
        @Index(name = "idx_subscription_overage_policy_plan", columnList = "plan_version_id,entitlement_definition_id"),
        @Index(name = "idx_subscription_overage_policy_mode", columnList = "mode,active")
})
public class SubscriptionOveragePolicy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_version_id", nullable = false, foreignKey = @ForeignKey(name = "fk_overage_policy_plan_version"))
    private PlanVersion planVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_definition_id", nullable = false, foreignKey = @ForeignKey(name = "fk_overage_policy_entitlement"))
    private EntitlementDefinition entitlementDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 40)
    private OveragePolicyMode mode;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "free_quantity", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal freeQuantity = BigDecimal.ZERO;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

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
