package com.sni.bokaticowork.features.billing.enums;

/** Etat d'un acompte (lot B3). */
public enum BillingDepositStatus {
    /** Prevu au plan, pas encore facture. */
    PLANNED,
    /** Facture d'acompte emise, rien d'encaisse. */
    INVOICED,
    PARTIALLY_PAID,
    PAID,
    /** Deduit de la facture finale. */
    DEDUCTED,
    /** Abandonne (devis refuse ou expire sans encaissement, retrait du plan). */
    CANCELLED,
    /** Encaisse puis rembourse au client (portefeuille). */
    REFUNDED,
    /** Encaisse puis garde en avoir pour le client. */
    CREDITED
}
