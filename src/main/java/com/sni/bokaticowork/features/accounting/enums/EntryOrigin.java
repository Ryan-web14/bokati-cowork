package com.sni.bokaticowork.features.accounting.enums;

/** Origine d'une piece. */
public enum EntryOrigin {
    /** Saisie manuelle. */
    MANUAL,
    /** Caisse. */
    CASH,
    /** Ecriture automatique de la plateforme. */
    PLATFORM,
    /** Import. */
    IMPORT,
    /** Cloture. */
    CLOSING,
    /** A-nouveaux. */
    OPENING,
    /** Contre-passation. */
    REVERSAL,
    /** Regularisation. */
    REGULARIZATION
}
