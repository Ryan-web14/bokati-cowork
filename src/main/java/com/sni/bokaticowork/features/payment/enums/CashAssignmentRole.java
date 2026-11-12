package com.sni.bokaticowork.features.payment.enums;

/** Role d'une personne sur une caisse (section 4.3). */
public enum CashAssignmentRole {
    /** Ouvre une session, encaisse, opere dans ses plafonds. */
    CASHIER,
    /** En plus : sorties au-dela du plafond caissier, transferts, creation de journal. */
    HEAD_CASHIER,
    /** Valide ecarts, ajustements et contre-passations ; comptages inopines ; cloture d'office. */
    SUPERVISOR,
    /** Lecture seulement : ne peut pas ouvrir de session. */
    VIEWER
}
