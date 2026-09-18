package com.sni.bokaticowork.features.subscription.subscription.enums;

public enum PassEventType {
    PASS_CREATED,
    PASS_ACTIVATED,
    PASS_RENEWED,
    PASS_SUSPENDED,
    PASS_CANCELLED,
    PASS_EXPIRED,
    PASS_PAST_DUE,
    /** Le pass a servi · le geste qui n'existait nulle part avant l'operationnalisation. */
    PASS_USED,

    /** Un usage a ete contre-passe. */
    PASS_USAGE_REVERSED,

    /** Le pass a change de titulaire. */
    PASS_TRANSFERRED,

    ENTITLEMENTS_GRANTED,
    BILLING_SCHEDULED,
    RENEWAL_SCHEDULED
}
