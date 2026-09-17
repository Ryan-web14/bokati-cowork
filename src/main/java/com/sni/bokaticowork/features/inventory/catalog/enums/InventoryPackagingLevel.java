package com.sni.bokaticowork.features.inventory.catalog.enums;

/**
 * Niveau d'un conditionnement, du plus petit au plus grand.
 */
public enum InventoryPackagingLevel {

    /** Unite de base du stock. */
    EACH(0),

    /** Regroupement intermediaire : blister, lot filme, botte. */
    INNER(1),

    /** Carton de manutention. */
    CASE(2),

    /** Palette ou unite de charge. */
    PALLET(3);

    private final int rank;

    InventoryPackagingLevel(int rank) {
        this.rank = rank;
    }

    /** Rang croissant, utilise pour ordonner les conditionnements d'un article. */
    public int getRank() {
        return rank;
    }
}
