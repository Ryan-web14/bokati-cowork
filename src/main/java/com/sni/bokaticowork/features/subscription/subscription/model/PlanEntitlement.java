package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "subscription_plan_entitlement", indexes = {
        @Index(name = "idx_plan_entitlement_version", columnList = "plan_version_id"),
        @Index(name = "idx_plan_entitlement_definition", columnList = "entitlement_definition_id")
})
public class PlanEntitlement {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_version_id", nullable = false, foreignKey = @ForeignKey(name = "fk_plan_entitlement_version"))
    private PlanVersion planVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_definition_id", nullable = false, foreignKey = @ForeignKey(name = "fk_plan_entitlement_definition"))
    private EntitlementDefinition entitlementDefinition;

    @Column(name = "quantity", precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unlimited", nullable = false)
    @Builder.Default
    private Boolean unlimited = Boolean.FALSE;

    @Column(name = "rollover_allowed", nullable = false)
    @Builder.Default
    private Boolean rolloverAllowed = Boolean.FALSE;

    @Column(name = "rollover_limit", precision = 19, scale = 4)
    private BigDecimal rolloverLimit;

    @Column(name = "valid_for_days")
    private Integer validForDays;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "restrictions_json", columnDefinition = "jsonb")
    private String restrictionsJson;

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
