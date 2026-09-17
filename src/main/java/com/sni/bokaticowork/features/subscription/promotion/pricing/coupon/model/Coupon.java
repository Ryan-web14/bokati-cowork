package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Le code que le client saisit, distinct de la campagne qui le porte.
 *
 * <p>La distinction n'est pas formelle. Une campagne dit ce qui est accorde et a quelles
 * conditions ; un coupon dit qui a le droit de le demander, combien de fois, et jusqu'a quand. Mille
 * codes peuvent porter la meme promotion avec des validites et des titulaires differents.</p>
 *
 * <p>Deux compteurs, et leur difference est le coeur du modele. {@code redemptionCount} compte ce
 * qui a ete reellement consomme ; {@code reservedCount} compte ce qui est retenu par des paniers en
 * cours. Un code retenu n'est pas consomme : si le panier est abandonne, la retenue tombe et le code
 * redevient disponible.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "coupon", indexes = {
        @Index(name = "idx_coupon_promotion", columnList = "promotion_id"),
        @Index(name = "idx_coupon_batch", columnList = "batch_id"),
        @Index(name = "idx_coupon_assignee", columnList = "assigned_to_type,assigned_to_code,status"),
        @Index(name = "idx_coupon_status_validity", columnList = "status,valid_from,valid_until")
})
public class Coupon {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private CouponBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_kind", nullable = false, length = 40)
    private CouponKind couponKind;

    /** Coupon nominatif, ou nul pour un code public. */
    @Column(name = "assigned_to_type", length = 60)
    private String assignedToType;

    @Column(name = "assigned_to_code", length = 120)
    private String assignedToCode;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "redemption_count", nullable = false)
    @Builder.Default
    private Integer redemptionCount = 0;

    @Column(name = "reserved_count", nullable = false)
    @Builder.Default
    private Integer reservedCount = 0;

    /** Peut etre plus courte que celle de la promotion porteuse, jamais plus longue en pratique. */
    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "issued_by", length = 120)
    private String issuedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @Column(name = "revoked_by", length = 120)
    private String revokedBy;

    /**
     * Verrou optimiste, en complement du verrou exclusif pris a la reservation. Deux paniers
     * simultanes ne peuvent pas consommer le meme code a usage unique, ce qui est precisement ce
     * qui arrive a un code partage sur les reseaux.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Nombre d'utilisations encore possibles, reservations en cours deduites. {@code null} lorsque
     * le code est illimite.
     */
    public Integer remainingUses() {
        Integer ceiling = effectiveMaxRedemptions();
        if (ceiling == null) {
            return null;
        }
        int used = (redemptionCount == null ? 0 : redemptionCount) + (reservedCount == null ? 0 : reservedCount);
        return Math.max(0, ceiling - used);
    }

    /**
     * Plafond reel. Un code a usage unique vaut un, quoi qu'on ait saisi dans
     * {@code maxRedemptions} : le genre prime sur la valeur, sans quoi une saisie a dix rendrait
     * l'appellation mensongere.
     */
    public Integer effectiveMaxRedemptions() {
        if (couponKind == CouponKind.SINGLE_USE) {
            return 1;
        }
        return maxRedemptions;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (issuedAt == null) {
            issuedAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
