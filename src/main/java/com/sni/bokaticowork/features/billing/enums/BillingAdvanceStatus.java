package com.sni.bokaticowork.features.billing.enums;

public enum BillingAdvanceStatus {
    PENDING,
    /** Une partie de l'acompte est reglee · le paiement impute sur la facture ne le couvre pas encore. */
    PARTIALLY_PAID,
    PAID,
    CANCELLED
}