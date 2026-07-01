package com.sni.bokaticowork.features.billing.enums;

public enum BillingDocumentType {
    QUOTE,
    PROFORMA_INVOICE,
    INVOICE,
    CREDIT_NOTE,
    /** Facture rectificative — corrige une erreur sur une facture validée (type, TVA, article). Séquence REC. */
    CORRECTIVE_INVOICE,
    DEBIT_NOTE
}
