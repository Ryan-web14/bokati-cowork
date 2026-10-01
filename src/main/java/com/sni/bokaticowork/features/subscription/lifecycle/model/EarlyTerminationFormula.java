package com.sni.bokaticowork.features.subscription.lifecycle.model;

/** Ce que coute de rompre un engagement avant son terme. */
public enum EarlyTerminationFormula {
    /** Rien · l'engagement n'a pas de penalite. */
    NONE,
    /** Un montant fixe, quel que soit le reste a courir. */
    FIXED_FEE,
    /** Un pourcentage des mensualites restantes. */
    PERCENT_OF_REMAINING,
    /** Toutes les mensualites restantes sont dues. */
    REMAINING_PERIODS
}
