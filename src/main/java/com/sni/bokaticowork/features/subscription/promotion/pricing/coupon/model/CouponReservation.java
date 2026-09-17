package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Retenue temporaire d'un code pendant qu'un panier se construit.
 *
 * <p>C'est la meme mecanique que la retenue de stock et que le blocage de portefeuille, et pour la
 * meme raison : ce qui est promis a quelqu'un ne doit pas etre promis a un autre, sans etre pour
 * autant deja preleve. Un code a usage unique consomme des la saisie serait perdu si le panier
 * etait abandonne.</p>
 *
 * <p>L'echeance n'est pas un detail de confort. Sans elle, un panier abandonne immobiliserait un
 * code jusqu'a la fin de la campagne.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "coupon_reservation", indexes = {
        @Index(name = "idx_coupon_reservation_coupon", columnList = "coupon_id,status"),
        @Index(name = "idx_coupon_reservation_cart", columnList = "cart_reference,status"),
        @Index(name = "idx_coupon_reservation_expiry", columnList = "status,expires_at")
})
public class CouponReservation {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "reservation_number", nullable = false, unique = true, length = 100)
    private String reservationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    /** Ce qui identifie le panier · une session, une intention de paiement, un numero de commande. */
    @Column(name = "cart_reference", nullable = false, length = 120)
    private String cartReference;

    @Column(name = "subscriber_type", length = 60)
    private String subscriberType;

    @Column(name = "subscriber_code", length = 120)
    private String subscriberCode;

    /** Montant annonce au client au moment de la retenue, pour verifier qu'il n'a pas bouge. */
    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private CouponReservationStatus status = CouponReservationStatus.RESERVED;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "reserved_by", length = 120)
    private String reservedBy;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "release_reason", length = 255)
    private String releaseReason;

    @PrePersist
    public void prePersist() {
        if (reservedAt == null) {
            reservedAt = Instant.now();
        }
    }
}
