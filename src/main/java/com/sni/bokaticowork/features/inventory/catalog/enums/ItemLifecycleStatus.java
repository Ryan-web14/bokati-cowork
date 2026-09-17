package com.sni.bokaticowork.features.inventory.catalog.enums;

/**
 * Cycle de vie d'un article, qui remplace le booleen {@code active} comme source de verite.
 *
 * <p>Le booleen reste renseigne pour compatibilite ascendante, calcule depuis ce statut.</p>
 */
public enum ItemLifecycleStatus {

    /** En cours de creation, invisible des operations de stock. */
    DRAFT(false, false, false),

    /** Nouvellement reference, deja operationnel. */
    NEW(true, true, true),

    /** En exploitation normale. */
    ACTIVE(true, true, true),

    /** Fin de serie : on ecoule le stock restant, on ne reapprovisionne plus. */
    PHASE_OUT(true, false, true),

    /** Obsolete : plus aucun mouvement, l'article n'existe que pour l'historique. */
    OBSOLETE(false, false, false),

    /** Bloque administrativement : ni entree ni sortie, en attente de decision. */
    BLOCKED(false, false, false);

    private final boolean active;
    private final boolean receivable;
    private final boolean issuable;

    ItemLifecycleStatus(boolean active, boolean receivable, boolean issuable) {
        this.active = active;
        this.receivable = receivable;
        this.issuable = issuable;
    }

    /** Valeur du booleen {@code active} correspondante. */
    public boolean isActive() {
        return active;
    }

    /** Vrai si l'article peut encore entrer en stock. */
    public boolean isReceivable() {
        return receivable;
    }

    /** Vrai si l'article peut encore sortir du stock. */
    public boolean isIssuable() {
        return issuable;
    }

    /**
     * Traduit un booleen {@code active} recu d'un client qui ignore le cycle de vie.
     *
     * <p>Volontairement grossier : un client qui n'envoie qu'un booleen ne peut pas exprimer la
     * nuance entre fin de serie, obsolescence et blocage. Pour cela il doit passer le statut.</p>
     */
    public static ItemLifecycleStatus fromActiveFlag(Boolean active) {
        return Boolean.FALSE.equals(active) ? OBSOLETE : ACTIVE;
    }
}
