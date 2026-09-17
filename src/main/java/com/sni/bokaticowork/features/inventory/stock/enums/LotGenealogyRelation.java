package com.sni.bokaticowork.features.inventory.stock.enums;

/**
 * Nature du lien entre un lot parent et un lot enfant.
 */
public enum LotGenealogyRelation {

    /** Le lot enfant resulte d une transformation du parent. */
    TRANSFORMATION,

    /** Reconditionnement sans transformation de la matiere. */
    REPACKAGING,

    /** Plusieurs lots parents fondus en un seul enfant. */
    MERGE,

    /** Un lot parent scinde en plusieurs enfants. */
    SPLIT,

    /** Deplacement d un emplacement a un autre, la matiere restant identique. */
    TRANSFER,

    /** Correction manuelle de rattachement. */
    CORRECTION
}
