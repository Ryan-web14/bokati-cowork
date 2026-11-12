package com.sni.bokaticowork.features.payment.enums;

public enum PaymentMethod {
    CASH,
    WALLET,
    MOBILE_MONEY,
    /**
     * Mobile money recu hors de la plateforme (MTN MoMo Pay, Airtel Money sur le numero marchand)
     * puis saisi au guichet. Ni PawaPay ni caisse automatique : un remboursement se fait a la main.
     */
    MOBILE_MONEY_OFFLINE,
    BANK_TRANSFER,
    CARD,
    CHEQUE,
    MANUAL_ADJUSTMENT,
    CREDIT_NOTE
}
