package com.sni.bokaticowork.features.payment.enums;

/** Imputation d'un ecart (decision 8 du plan). */
public enum CashVarianceDecision {
    /** Charge (manquant, compte 658). */
    EXPENSE,
    /** Produit (excedent, compte 758). */
    INCOME,
    /** Imputation au caissier (compte 421, retenue possible sur paie). */
    CASHIER,
    /** Mise en recherche : reste en compte d'attente 471 avec une echeance. */
    INVESTIGATION,
    /** Ecart dans la tolerance, impute en charge ou produit sans revue. */
    TOLERATED
}
