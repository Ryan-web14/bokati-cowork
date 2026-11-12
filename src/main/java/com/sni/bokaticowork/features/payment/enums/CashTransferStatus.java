package com.sni.bokaticowork.features.payment.enums;

/** Etat d'un ordre de transfert interne (section 8). */
public enum CashTransferStatus {
    /** Envoye, pas encore recu : l'argent est en route (compte 585). */
    IN_TRANSIT,
    /** Recu pour le montant envoye. */
    RECEIVED,
    /** Recu pour un autre montant : en revue. */
    DISPUTED,
    /** Annule par l'emetteur avant reception ; la sortie est contre-passee. */
    CANCELLED
}
