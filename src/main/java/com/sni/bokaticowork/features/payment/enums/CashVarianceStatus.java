package com.sni.bokaticowork.features.payment.enums;

/** Etat d'une revue d'ecart. */
public enum CashVarianceStatus {
    /** En attente de decision. */
    PENDING,
    /** Decidee. */
    DECIDED,
    /** Dans la tolerance : comptabilisee sans revue. */
    TOLERATED
}
