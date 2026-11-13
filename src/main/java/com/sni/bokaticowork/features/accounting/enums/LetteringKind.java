package com.sni.bokaticowork.features.accounting.enums;

/** Origine d'un lettrage. */
public enum LetteringKind {
    /** Automatique par reference. */
    AUTO_REFERENCE,
    /** Automatique par montant. */
    AUTO_MATCH,
    /** Manuel. */
    MANUAL,
    /** Reprise en a-nouveau a la cloture : la ligne de l'exercice clos est soldee, sa suite est la ligne d'a-nouveau (lot F12). */
    CARRY_FORWARD
}
