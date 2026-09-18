package com.sni.bokaticowork.features.payment.enums;

public enum WalletEntryType {
    ADMIN_TOPUP,
    /** Rechargement a l'initiative du titulaire · distinct de ADMIN_TOPUP, qui est un geste de guichet. */
    TOPUP,
    TRANSFER_IN,
    TRANSFER_OUT,
    /** Frais preleves sur l'emetteur d'un transfert · une ecriture a part, jamais fondue dans le montant. */
    TRANSFER_FEE,
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
