package com.sni.bokaticowork.features.payment.enums;

public enum WalletEntryType {
    ADMIN_TOPUP,
    /** Rechargement a l'initiative du titulaire · distinct de ADMIN_TOPUP, qui est un geste de guichet. */
    TOPUP,
    TRANSFER_IN,
    TRANSFER_OUT,
    ADMIN_DEBIT,
    PAYMENT,
    REFUND,
    REVERSAL,
    HOLD,
    HOLD_RELEASE,
    ADJUSTMENT,
    CASHBACK,
    PROMOTIONAL_CREDIT,
    OVERPAYMENT_CREDIT
}
