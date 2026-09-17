package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lot de generation.
 *
 * <p>Besoin concret et non theorique : mille codes uniques pour un partenariat, imprimes sur des
 * flyers, avec un suivi du taux d'utilisation par lot. Sans regroupement, mille lignes de coupons
 * ne disent rien de la campagne qui les a produits.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "coupon_batch", indexes = {
        @Index(name = "idx_coupon_batch_promotion", columnList = "promotion_id")
})
public class CouponBatch {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "batch_code", nullable = false, unique = true, length = 100)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "generated_quantity", nullable = false)
    @Builder.Default
    private Integer generatedQuantity = 0;

    @Column(name = "redeemed_quantity", nullable = false)
    @Builder.Default
    private Integer redeemedQuantity = 0;

    @Column(name = "code_prefix", length = 40)
    private String codePrefix;

    @Column(name = "code_length", nullable = false)
    @Builder.Default
    private Integer codeLength = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_kind", nullable = false, length = 40)
    private CouponKind couponKind;

    @Column(name = "max_redemptions_per_coupon")
    private Integer maxRedemptionsPerCoupon;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    /** Canal de diffusion, pour mesurer ce qui marche · flyer, courriel, partenaire. */
    @Column(name = "channel", length = 60)
    private String channel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 120)
    private String createdBy;

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
