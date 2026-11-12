package com.sni.bokaticowork.features.accounting.enums;

/** Type de journal. */
public enum JournalType {
    /** Ventes. */
    SALES,
    /** Achats. */
    PURCHASES,
    /** Caisse. */
    CASH,
    /** Banque. */
    BANK,
    /** Mobile money. */
    MOBILE_MONEY,
    /** Portefeuille electronique. */
    WALLET,
    /** Operations diverses. */
    MISC,
    /** Stock. */
    STOCK,
    /** A-nouveaux. */
    OPENING,
    /** Extournes. */
    REVERSAL,
    /** Hors bilan (classe 90). */
    OFF_BALANCE,
    /** Comptabilite analytique (classe 9). */
    ANALYTIC,
    /** Paie. */
    PAYROLL
}
