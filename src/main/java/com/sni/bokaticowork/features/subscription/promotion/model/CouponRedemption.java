package com.sni.bokaticowork.features.subscription.promotion.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "coupon_redemption", indexes = {
        @Index(name = "idx_coupon_redemption_promotion", columnList = "promotion_id"),
        @Index(name = "idx_coupon_redemption_subscriber", columnList = "subscriber_type,subscriber_code")
})
public class CouponRedemption {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_coupon_redemption_promotion"))
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type", nullable = false, length = 60)
    private SubscriberType subscriberType;

    @Column(name = "subscriber_code", nullable = false, length = 120)
    private String subscriberCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", foreignKey = @ForeignKey(name = "fk_coupon_redemption_subscription"))
    private Subscription subscription;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt;

    @PrePersist
    public void prePersist() {
        if (redeemedAt == null) {
            redeemedAt = Instant.now();
        }
    }
}
