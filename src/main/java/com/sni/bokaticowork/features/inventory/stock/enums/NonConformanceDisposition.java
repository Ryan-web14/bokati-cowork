package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Ce que l on decide de faire de la marchandise non conforme.
 */
public enum NonConformanceDisposition {

    /** Accepte en l etat. */
    USE_AS_IS,

    /** Reprise ou remise en conformite. */
    REWORK,

    /** Retour au fournisseur. */
    RETURN_TO_SUPPLIER,

    /** Mise au rebut. */
    SCRAP,

    /** Declassement vers un usage moins exigeant. */
    DOWNGRADE,

    /** En attente de decision. */
    PENDING
}
