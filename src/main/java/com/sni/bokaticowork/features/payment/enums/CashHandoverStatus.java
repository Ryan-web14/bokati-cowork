package com.sni.bokaticowork.features.payment.enums;

/** Etat d'une passation. */
public enum CashHandoverStatus {
    /** Comptage du sortant saisi, en attente de l'entrant. */
    PENDING,
    /** Accepte par l'entrant. */
    ACCEPTED,
    /** Conteste par l'entrant : en revue. */
    DISPUTED,
    /** Annulee par le sortant. */
    CANCELLED
}
