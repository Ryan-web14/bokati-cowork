package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Pourquoi un lot ne peut plus sortir du stock.
 *
 * <p>La quarantaine est un etat d attente, decide par le controle qualite. Le blocage est une
 * decision administrative. Les deux rendent le lot indisponible, mais ne se levent pas de la meme
 * facon ni par les memes personnes.</p>
 */
public enum LotBlockReasonType {

    /** En attente de controle qualite. */
    PENDING_INSPECTION,

    /** Controle non conforme, en attente de decision. */
    FAILED_INSPECTION,

    /** Litige avec le fournisseur. */
    SUPPLIER_DISPUTE,

    /** Rappel produit en cours. */
    RECALL,

    /** Peremption atteinte ou imminente. */
    EXPIRY,

    /** Decision administrative sans autre categorie. */
    ADMINISTRATIVE
}
