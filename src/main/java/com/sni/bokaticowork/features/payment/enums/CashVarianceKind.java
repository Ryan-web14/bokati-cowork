package com.sni.bokaticowork.features.payment.enums;

/** Origine d'un ecart en revue. */
public enum CashVarianceKind {
    /** Ecart entre le fonds compte a l'ouverture et le dernier comptage. */
    OPENING,
    /** Ecart de cloture. */
    CLOSING,
    /** Ecart constate par un comptage inopine. */
    SPOT_CHECK,
    /** Desaccord lors d'une passation. */
    HANDOVER,
    /** Ecart d'une cloture d'office, contestable par le caissier. */
    FORCED_CLOSE,
    /** Ecart a l'ouverture d'un scelle. */
    UNSEAL,
    /** Ecart de transfert interne. */
    TRANSFER,
    /** Ecart a la remise de caisse. */
    RESPONSIBILITY_TRANSFER
}
