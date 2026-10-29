package com.sni.bokaticowork.features.company.enums;

/**
 * Comment les factures d'un membre se relient a son organisation.
 *
 * <p>Regle par defaut sur l'organisation, remplacable sur chaque lien. Lu a l'emission de la
 * facture · le changer ne touche pas aux factures deja emises.</p>
 */
public enum InvoiceLinkMode {
    /** Toute facture du membre est reliee d'office a l'organisation tant que le lien est actif. */
    AUTOMATIC,
    /** Le membre ou l'accueil choisit, a chaque fois, « pour moi » ou « pour l'organisation ». */
    ON_CHOICE
}
