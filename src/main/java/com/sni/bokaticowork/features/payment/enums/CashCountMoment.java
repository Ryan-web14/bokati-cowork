package com.sni.bokaticowork.features.payment.enums;

/** Moment d'un comptage (section 6.1). */
public enum CashCountMoment {
    /** Ouverture de session. */
    OPENING,
    /** Cloture de session. */
    CLOSING,
    /** Recomptage par un second compteur. */
    RECOUNT,
    /** Passation entre deux caissiers. */
    HANDOVER,
    /** Comptage inopine par un superviseur. */
    SPOT_CHECK,
    /** Reception d'un transfert interne. */
    TRANSFER_RECEIPT,
    /** Comptage periodique (coffre). */
    PERIODIC,
    /** Remise de caisse a un nouveau titulaire. */
    RESPONSIBILITY_TRANSFER,
    /** Cloture d'office. */
    FORCED_CLOSE,
    /** Mise sous scelles. */
    SEAL,
    /** Ouverture du scelle. */
    UNSEAL
}
