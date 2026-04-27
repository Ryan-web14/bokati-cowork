package com.sni.bokaticowork.features.subscription.overage.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;
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
@Table(name = "subscription_overage_charge", indexes = {
        @Index(name = "idx_subscription_overage_charge_subscription", columnList = "subscription_id,created_at"),
        @Index(name = "idx_subscription_overage_charge_owner", columnList = "owner_type,owner_code,created_at"),
        @Index(name = "idx_subscription_overage_charge_usage", columnList = "usage_record_id")
})
public class SubscriptionOverageCharge {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "charge_number", nullable = false, unique = true, length = 100)
    private String chargeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", foreignKey = @ForeignKey(name = "fk_overage_charge_subscription"))
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usage_record_id", nullable = false, foreignKey = @ForeignKey(name = "fk_overage_charge_usage"))
    private UsageRecord usageRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billable_item_id", foreignKey = @ForeignKey(name = "fk_overage_charge_billable_item"))
    private BillableItem billableItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @Column(name = "entitlement_code", nullable = false, length = 100)
    private String entitlementCode;

    @Column(name = "overage_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal overageQuantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
