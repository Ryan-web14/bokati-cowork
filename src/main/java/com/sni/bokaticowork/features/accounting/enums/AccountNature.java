package com.sni.bokaticowork.features.accounting.enums;

/** Nature d'un compte. */
public enum AccountNature {
    /** Bilan (classes 1 a 5). */
    BALANCE,
    /** Gestion (classes 6 et 7). */
    MANAGEMENT,
    /** Hors activites ordinaires (classe 8). */
    HAO,
    /** Comptabilite analytique (classe 9, 91 a 99). */
    ANALYTIC,
    /** Engagements hors bilan (classe 90). */
    OFF_BALANCE
}
