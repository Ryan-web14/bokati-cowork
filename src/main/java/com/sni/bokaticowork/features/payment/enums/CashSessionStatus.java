package com.sni.bokaticowork.features.payment.enums;

/** Etats d'une session de caisse (plan des caisses, section 5.1). */
public enum CashSessionStatus {
    /** Ouverture en cours : ecart d'ouverture a confirmer avec un motif. */
    OPENING,
    /** Ouverte : operations possibles. */
    OPEN,
    /** En pause : aucune operation ; reprise par le meme caissier, mot de passe redemande. */
    PAUSED,
    /** Suspendue par un superviseur (ou par la fin de mois, une absence) : seul un superviseur la leve. */
    SUSPENDED,
    /** Cloture demandee : plus d'operation, comptage en cours. */
    COUNTING,
    /** Comptee, ecart hors tolerance en attente de decision. */
    CLOSING_REVIEW,
    /** Recomptage demande : un second compteur, different du premier, compte. */
    RECOUNT,
    CLOSED,
    /** Ouverte par erreur sans operation : annulee, sans trace financiere. */
    CANCELLED;

    /** La session occupe-t-elle encore sa caisse et son caissier ? */
    public boolean isLive() {
        return this != CLOSED && this != CANCELLED;
    }
}
