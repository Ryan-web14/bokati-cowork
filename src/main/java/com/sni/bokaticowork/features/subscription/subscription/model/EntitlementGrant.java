package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
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
@Table(name = "entitlement_grant", indexes = {
        @Index(name = "idx_entitlement_grant_number", columnList = "grant_number"),
        @Index(name = "idx_entitlement_grant_owner", columnList = "owner_type,owner_code,status"),
        @Index(name = "idx_entitlement_grant_valid_until", columnList = "valid_until"),
        @Index(name = "idx_entitlement_grant_subscription", columnList = "subscription_id"),
        @Index(name = "idx_entitlement_grant_pass", columnList = "pass_id")
})
public class EntitlementGrant {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "grant_number", nullable = false, unique = true, length = 100)
    private String grantNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", foreignKey = @ForeignKey(name = "fk_entitlement_grant_subscription"))
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pass_id", foreignKey = @ForeignKey(name = "fk_entitlement_grant_pass"))
    private Pass pass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_definition_id", nullable = false, foreignKey = @ForeignKey(name = "fk_entitlement_grant_definition"))
    private EntitlementDefinition entitlementDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @Column(name = "quantity_granted", precision = 19, scale = 4)
    private BigDecimal quantityGranted;

    @Column(name = "quantity_remaining", precision = 19, scale = 4)
    private BigDecimal quantityRemaining;

    @Column(name = "unlimited", nullable = false)
    @Builder.Default
    private Boolean unlimited = Boolean.FALSE;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private EntitlementGrantStatus status = EntitlementGrantStatus.ACTIVE;

    @Column(name = "source_type", nullable = false, length = 80)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 120)
    private String sourceId;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "alert_80_sent_at")
    private Instant alert80SentAt;

    @Column(name = "alert_100_sent_at")
    private Instant alert100SentAt;

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
