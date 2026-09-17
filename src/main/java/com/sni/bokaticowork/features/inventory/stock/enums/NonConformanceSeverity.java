package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Gravite d une non-conformite.
 */
public enum NonConformanceSeverity {

    /** Ecart mineur, sans consequence sur l usage. */
    MINOR,

    /** Ecart notable, usage degrade ou restreint. */
    MAJOR,

    /** Ecart bloquant, produit inutilisable ou dangereux. */
    CRITICAL
}
