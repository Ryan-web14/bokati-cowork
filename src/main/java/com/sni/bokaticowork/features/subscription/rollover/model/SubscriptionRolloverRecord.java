package com.sni.bokaticowork.features.subscription.rollover.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
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
@Table(name = "subscription_rollover_record", indexes = {
        @Index(name = "idx_subscription_rollover_subscription", columnList = "subscription_id,created_at"),
        @Index(name = "idx_subscription_rollover_source", columnList = "source_grant_id"),
        @Index(name = "idx_subscription_rollover_grant", columnList = "rollover_grant_id")
})
public class SubscriptionRolloverRecord {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "rollover_number", nullable = false, unique = true, length = 100)
    private String rolloverNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rollover_subscription"))
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_grant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rollover_source_grant"))
    private EntitlementGrant sourceGrant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rollover_grant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rollover_new_grant"))
    private EntitlementGrant rolloverGrant;

    @Column(name = "entitlement_code", nullable = false, length = 100)
    private String entitlementCode;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
