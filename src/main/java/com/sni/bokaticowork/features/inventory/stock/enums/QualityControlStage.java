package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Moment auquel un controle qualite s applique.
 */
public enum QualityControlStage {

    /** A la reception de la marchandise. */
    ON_RECEIPT,

    /** Pendant le stockage, a intervalle regulier. */
    IN_STORAGE,

    /** Juste avant la sortie du stock. */
    BEFORE_ISSUE,

    /** Sur declenchement manuel. */
    ON_DEMAND
}
