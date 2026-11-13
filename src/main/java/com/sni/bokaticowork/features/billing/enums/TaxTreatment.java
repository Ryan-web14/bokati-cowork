package com.sni.bokaticowork.features.billing.enums;

/** Lot C0 : traitement fiscal d'une regle de taxe, lu par le calcul des lignes (lot C1). */
public enum TaxTreatment {
    /** Taux normal. */
    STANDARD,
    /** Taux reduit. */
    REDUCED,
    /** Operation exoneree : ni TVA ni centimes additionnels, motif imprime. */
    EXEMPT,
    /** Operation hors du champ de la TVA. */
    OUT_OF_SCOPE
}
