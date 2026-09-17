package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Avancement d un rappel produit.
 */
public enum ProductRecallStatus {

    /** Rappel prepare, lots pas encore geles. */
    DRAFT,

    /** Rappel lance, lots geles, detenteurs notifies. */
    ACTIVE,

    /** Rappel termine. */
    CLOSED,

    /** Rappel annule, lots liberes. */
    CANCELLED
}
