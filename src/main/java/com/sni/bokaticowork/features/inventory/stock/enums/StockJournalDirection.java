package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Sens de l'ecriture sur le compte de stock.
 */
public enum StockJournalDirection {

    /** Le stock augmente : le compte de stock est debite. */
    DEBIT,

    /** Le stock diminue : le compte de stock est credite. */
    CREDIT
}
