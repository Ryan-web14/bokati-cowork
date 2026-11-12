package com.sni.bokaticowork.features.accounting.enums;

/** Politique pour une piece desequilibree (accounting.unbalanced-entry.policy). */
public enum UnbalancedPolicy {
    /** Mise en attente d'equilibrage, notifiee (defaut). */
    HOLD,
    /** Ligne d'equilibrage sur le compte d'attente dedie, puis validation. */
    SUSPENSE_ACCOUNT,
    /** Validation telle quelle avec le marqueur accepted_unbalanced ; bloque la cloture. */
    ACCEPT
}
