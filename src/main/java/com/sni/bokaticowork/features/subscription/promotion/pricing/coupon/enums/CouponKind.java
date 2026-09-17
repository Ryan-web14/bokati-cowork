package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums;

/**
 * Ce qu'un coupon autorise.
 *
 * <p>Un code public partage sur les reseaux et un code nominatif a usage unique sont deux objets
 * differents dans la vie reelle. Les distinguer par un champ evite d'en faire deux entites.</p>
 */
public enum CouponKind {

    /** Une seule utilisation, par qui que ce soit. Le premier arrive l'emporte. */
    SINGLE_USE,

    /** Plusieurs utilisations, dans la limite de {@code maxRedemptions}. */
    MULTI_USE,

    /** Plusieurs utilisations, mais une seule par abonne. Le code de parrainage type. */
    UNIQUE_PER_SUBSCRIBER
}
