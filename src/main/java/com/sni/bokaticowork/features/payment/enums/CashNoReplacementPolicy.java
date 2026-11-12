package com.sni.bokaticowork.features.payment.enums;

/** Que faire du fonds quand un absent n'a pas de remplacant. */
public enum CashNoReplacementPolicy {
    /** Remise a la caisse mere, caisse suspendue jusqu'au retour. */
    REMIT_TO_PARENT,
    /** Mise sous scelles et depot au coffre. */
    SEAL,
    /** Suspension simple, sous un plafond. */
    SIMPLE_SUSPENSION
}
