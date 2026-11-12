package com.sni.bokaticowork.features.payment.enums;

public enum CashMovementType {
    OPENING_FLOAT,
    PAYMENT,
    REFUND,
    CASH_IN,
    CASH_OUT,
    SAFE_DEPOSIT,
    TRANSFER_IN,
    TRANSFER_OUT,
    ADJUSTMENT,
    CLOSING_COUNT,
    /** Echange de monnaie (gros billets contre petites coupures) : montant net nul, neutre. */
    EXCHANGE,
    /** Ecart constate (ouverture, cloture, comptage) produit par le systeme ; porte un sens. */
    VARIANCE
}
