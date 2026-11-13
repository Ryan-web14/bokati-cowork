package com.sni.bokaticowork.features.billing.enums;

public enum BillingDocumentStatus {
    DRAFT,
    ISSUED,
    /** Facture fiscalement scellée (SEFC) · immuable, numéro définitif assigné. */
    VALIDATED,
    /** Facture : envoi trace sans changer le statut depuis le lot B1 ; reste pour l'historique. */
    SENT,
    /** Devis ouvert par le client (tracking email). */
    VIEWED,
    /** Devis en cours de négociation. */
    NEGOTIATION,
    ACCEPTED,
    /** Acompte demandé avant conversion. */
    DEPOSIT_REQUESTED,
    /** Acompte reçu, en attente de livraison / conversion. */
    DEPOSIT_PAID,
    REJECTED,
    EXPIRED,
    CONVERTED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED,
    /** Avoir reverse au portefeuille du client (ou rembourse). */
    REFUNDED,
    /** Avoir impute sur une facture : consomme. */
    APPLIED,
    WRITTEN_OFF
}
