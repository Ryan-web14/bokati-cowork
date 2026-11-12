package com.sni.bokaticowork.features.accounting.enums;

/** Statut d'une periode. */
public enum PeriodStatus {
    /** Ouverte. */
    OPEN,
    /** Verrouillee : plus d'ecriture automatique, saisie comptable seulement. */
    LOCKED,
    /** Close : n'accepte plus rien. */
    CLOSED
}
