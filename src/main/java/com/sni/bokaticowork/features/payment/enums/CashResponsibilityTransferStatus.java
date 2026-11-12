package com.sni.bokaticowork.features.payment.enums;

/** Etat d'un transfert de responsabilite (PV de remise de caisse). */
public enum CashResponsibilityTransferStatus {
    /** Cree, affectations deja mises a jour, en attente des deux signatures. */
    PENDING_SIGNATURES,
    /** Signe par le sortant (ou le superviseur) et par l'entrant. */
    SIGNED,
    /** Annule avant la date d'effet. */
    CANCELLED
}
