package com.sni.bokaticowork.features.accounting.enums;

/** Motif d'une mise en attente. */
public enum PendingReason {
    /** Piece desequilibree. */
    UNBALANCED,
    /** Contrepartie non conforme. */
    COUNTERPART_NOT_ALLOWED,
    /** Compte manquant. */
    MISSING_ACCOUNT,
    /** Ventilation analytique incomplete. */
    ANALYTIC_INCOMPLETE,
    /** Compte inactif. */
    INACTIVE_ACCOUNT
}
