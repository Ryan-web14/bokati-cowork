package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Issue d un controle qualite.
 */
public enum QualityDecision {

    /** Lot conforme, libere. */
    ACCEPTED,

    /** Lot refuse, a retourner ou a rebuter. */
    REJECTED,

    /** Lot immobilise en attente de decision. */
    QUARANTINED,

    /** Lot accepte malgre une non-conformite, sous responsabilite explicite. */
    ACCEPTED_BY_DEROGATION
}
