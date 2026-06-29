package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_plan_entitlement", indexes = {
        @Index(name = "idx_pass_plan_ent_version", columnList = "pass_plan_version_id")
})
public class PassPlanEntitlement {

    @Id
    @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_plan_version_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pass_plan_ent_version"))
    private PassPlanVersion passVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_definition_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pass_plan_ent_definition"))
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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}
