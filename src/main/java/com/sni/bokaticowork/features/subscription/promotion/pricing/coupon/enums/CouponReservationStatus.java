package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums;

/**
 * Cycle d'une retenue de coupon.
 *
 * <p>Le coupon n'est consomme qu'a la capture. Un code a usage unique consomme des la saisie serait
 * perdu si le panier est abandonne, et le client n'aurait plus rien a montrer.</p>
 */
public enum CouponReservationStatus {
    RESERVED,
    CAPTURED,
    RELEASED,
    EXPIRED
}
