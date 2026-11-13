package com.sni.bokaticowork.features.billing.enums;

/** Etat d'une demande de modification de facture (lot B14). */
public enum BillingChangeRequestStatus {
    /** En attente d'approbation : la facture garde sa version en vigueur. */
    PENDING,
    /** Approuvee et appliquee (nouvelle version ou rectificative). */
    APPLIED,
    REJECTED,
    /** Renvoyee pour correction : le demandeur en soumet une nouvelle. */
    RETURNED,
    CANCELLED
}
