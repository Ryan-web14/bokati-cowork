package com.sni.bokaticowork.features.subscription.promotion.pricing.enums;

/** Comment une promotion se declenche. */
public enum PromotionType {

    /** S'applique d'elle-meme des que ses conditions sont reunies, sans que le client fasse rien. */
    AUTOMATIC,

    /** Exige la saisie d'un code par le client. */
    COUPON,

    /** Reservee aux beneficiaires nommement designes. */
    TARGETED
}
