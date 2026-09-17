package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums;

public enum ReferralStatus {

    /** Enregistre, pas encore qualifie. Aucune recompense n'est due a ce stade. */
    PENDING,

    /** La condition est remplie, les recompenses peuvent etre accordees. */
    QUALIFIED,

    /** Les deux recompenses ont ete accordees. */
    REWARDED,

    /** Ecarte · plafond atteint, programme clos, ou soupcon d'abus. */
    REJECTED
}
