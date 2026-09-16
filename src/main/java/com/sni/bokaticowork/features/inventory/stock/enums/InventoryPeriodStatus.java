package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Etat d'une periode comptable de stock.
 */
public enum InventoryPeriodStatus {

    /** Les mouvements sont acceptes. */
    OPEN(true),

    /** Cloture en cours : plus aucun mouvement, le temps de lever les ecarts. */
    CLOSING(false),

    /** Periode close. Aucun mouvement ne peut plus y etre date. */
    CLOSED(false);

    private final boolean acceptsMovements;

    InventoryPeriodStatus(boolean acceptsMovements) {
        this.acceptsMovements = acceptsMovements;
    }

    public boolean acceptsMovements() {
        return acceptsMovements;
    }
}
