package com.sni.bokaticowork.features.subscription.subscription.enums;

public enum SubscriptionStatus {
    DRAFT,
    PENDING_ACTIVATION,
    /** Des pieces manquent avant l'activation · le niveau KYC du plan n'est pas atteint. */
    PENDING_DOCUMENTS,
    TRIALING,
    ACTIVE,
    /** Echeance impayee · les droits restent ouverts le temps de la tolerance, avant suspension. */
    GRACE_PERIOD,
    PAST_DUE,
    PAUSED,
    SUSPENDED,
    /** Un preavis de resiliation court · l'abonnement vit jusqu'a sa date d'effet. */
    PENDING_TERMINATION,
    CANCELLED,
    EXPIRED;

    /** Les statuts ou l'abonne beneficie encore de ses droits. */
    public boolean entitled() {
        return this == TRIALING || this == ACTIVE || this == GRACE_PERIOD || this == PAST_DUE || this == PENDING_TERMINATION;
    }

    /** Les statuts qui ne sont pas termines · un abonnement dont on peut encore parler au present. */
    public boolean open() {
        return this != CANCELLED && this != EXPIRED;
    }
}
