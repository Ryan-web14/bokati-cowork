package com.sni.bokaticowork.features.accounting.enums;

/** Statut d'une piece comptable. */
public enum EntryStatus {
    /** Brouillard : numero provisoire, modifiable. */
    DRAFT,
    /** En attente d'equilibrage : hors comptabilite, notifiee. */
    PENDING_BALANCE,
    /** Validee : numero definitif, immuable. */
    VALIDATED,
    /** Validee puis contre-passee. */
    REVERSED,
    /** Rejetee avec motif, reste consultable. */
    REJECTED
}
