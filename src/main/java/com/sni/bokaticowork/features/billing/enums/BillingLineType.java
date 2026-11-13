package com.sni.bokaticowork.features.billing.enums;

public enum BillingLineType {
    PRODUCT,
    SERVICE,
    SUBSCRIPTION,
    PASS,
    ADDON,
    BOOKING,
    OVERAGE,
    PENALTY,
    WALLET,
    ADJUSTMENT,
    /**
     * Lot C10 : debours, frais avances pour le compte du client et refactures a l'identique (frais
     * de dossier, timbres, formalites). Hors TVA, hors remise globale, hors chiffre d'affaires.
     */
    DISBURSEMENT
}
