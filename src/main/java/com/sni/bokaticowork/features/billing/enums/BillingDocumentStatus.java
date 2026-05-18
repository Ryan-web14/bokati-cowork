package com.sni.bokaticowork.features.billing.enums;

public enum BillingDocumentStatus {
    DRAFT,
    ISSUED,
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
    VOIDED,
    REFUNDED,
    WRITTEN_OFF
}
