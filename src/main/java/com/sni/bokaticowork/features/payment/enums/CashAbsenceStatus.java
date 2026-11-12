package com.sni.bokaticowork.features.payment.enums;

/** Etat d'une absence de caissier. */
public enum CashAbsenceStatus {
    /** Declaree, a valider. */
    PLANNED,
    /** Validee, a venir. */
    APPROVED,
    /** En cours. */
    ACTIVE,
    /** Terminee. */
    ENDED,
    /** Annulee. */
    CANCELLED
}
