package com.sni.bokaticowork.features.accounting.enums;

/** Statut d'un compte. */
public enum AccountStatus {
    /** Cree, en attente de validation. */
    PENDING_APPROVAL,
    /** Utilisable. */
    ACTIVE,
    /** Desactive : garde son historique, ne recoit plus d'ecriture. */
    INACTIVE,
    /** Fusionne dans un autre compte. */
    MERGED
}
