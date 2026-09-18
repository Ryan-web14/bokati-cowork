package com.sni.bokaticowork.features.subscription.notification.enums;

public enum SubscriptionNotificationType {
    RENEWAL_UPCOMING,
    PAYMENT_DUE,
    OVERAGE_WARNING,
    OVERAGE_BILLED,
    ROLLOVER_APPLIED,
    PAUSE_STARTED,
    PAUSE_ENDING,
    CONTRACT_EXPIRING,
    FRAUD_REVIEW,
    WALLET_CREDIT_APPLIED,
    ENTITLEMENT_LOW_BALANCE,
    ENTITLEMENT_EXHAUSTED,
    SUBSCRIPTION_CANCELLED,

    /** Le pass arrive a echeance · le titulaire l'apprenait le jour ou il ne marchait plus. */
    PASS_EXPIRING,

    /** Solde de pass presque epuise. */
    PASS_LOW_BALANCE,

    /** Pass achete et jamais utilise · celle des trois qui coute a la relation, pas au client. */
    PASS_UNUSED
}
