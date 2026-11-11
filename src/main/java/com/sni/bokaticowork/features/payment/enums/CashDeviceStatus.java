package com.sni.bokaticowork.features.payment.enums;

/** Statut d'un poste vis-a-vis d'une caisse. */
public enum CashDeviceStatus {
    /** Vu a l'ouverture d'une session, en attente de decision ; n'empeche rien tant qu'aucun poste n'est autorise. */
    PROPOSED,
    /** Poste autorise : des qu'il y en a un, seuls les postes autorises peuvent ouvrir une session. */
    ALLOWED,
    /** Poste refuse. */
    REFUSED
}
