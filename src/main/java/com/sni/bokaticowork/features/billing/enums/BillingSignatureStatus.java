package com.sni.bokaticowork.features.billing.enums;

public enum BillingSignatureStatus {
    PENDING,
    SIGNED,
    EXPIRED,
    /** Devis revise ou sorti du circuit depuis la demande : la signature n'est plus possible. */
    CANCELLED
}
