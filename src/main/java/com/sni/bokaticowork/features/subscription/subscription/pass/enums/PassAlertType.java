package com.sni.bokaticowork.features.subscription.subscription.pass.enums;

/** Ce qu'une alerte de pass signale. */
public enum PassAlertType {

    /** L'echeance approche. */
    EXPIRING,

    /** Le solde restant descend sous le seuil. */
    LOW_BALANCE,

    /** Achete, actif, et jamais utilise. */
    UNUSED
}
