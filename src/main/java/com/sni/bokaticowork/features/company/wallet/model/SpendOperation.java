package com.sni.bokaticowork.features.company.wallet.model;

/**
 * Ce qu'une depense sur le portefeuille de l'organisation paie.
 */
public enum SpendOperation {
    /** Une facture de reservation. */
    BOOKING,
    /** Une facture d'abonnement, ou de depassement sur un abonnement. */
    SUBSCRIPTION,
    /** Une facture de pass. */
    PASS,
    /** Toute autre facture de l'organisation ou reliee a elle. */
    INVOICE_PAYMENT,
    /** Une commande de la marketplace (tranche 7). */
    MARKETPLACE
}
