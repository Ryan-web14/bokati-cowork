package com.sni.bokaticowork.features.payment.enums;

public enum PaymentIntentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    CANCELLED,
    EXPIRED,
    REFUNDED,
    /** Reglee puis remboursee en partie (17/11/2026 : enfin atteint). */
    PARTIALLY_REFUNDED,
    REVERSED
}
