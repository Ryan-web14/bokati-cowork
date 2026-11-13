package com.sni.bokaticowork.features.billing.enums;

/** Etape d'une affaire (lot B11). */
public enum BillingDealStage {
    PROSPECTING,
    PROPOSAL,
    NEGOTIATION,
    WON,
    /** Perdue, avec motif. */
    LOST
}
