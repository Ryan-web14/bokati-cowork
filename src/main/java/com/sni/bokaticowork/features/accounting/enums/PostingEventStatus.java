package com.sni.bokaticowork.features.accounting.enums;

/** Etat d'un evenement comptable dans le registre d'idempotence. */
public enum PostingEventStatus {
    /** Recu, pas encore comptabilise (comptabilite inactive, famille coupee, ou en file). */
    PENDING,
    /** Piece produite (validee, ou en brouillard selon le journal ou le mode reprise). */
    POSTED,
    /** Piece produite mais en attente d'equilibrage. */
    UNBALANCED,
    /** Echec technique : schema absent, donnee illisible, variable non resolue. */
    FAILED,
    /** Ecarte : aucun ecriture a passer (schema sans ecriture, ou decision d'un comptable). */
    IGNORED
}
