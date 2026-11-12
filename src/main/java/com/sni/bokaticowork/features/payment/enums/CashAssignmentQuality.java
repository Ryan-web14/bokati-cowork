package com.sni.bokaticowork.features.payment.enums;

/** Qualite d'une affectation (section 4.5). */
public enum CashAssignmentQuality {
    /** Titulaire : responsable du fonds ; un seul a la fois (deux pour un coffre). */
    HOLDER,
    /** Remplacant permanent, classe par ordre de preference. */
    BACKUP,
    /** Interimaire sur une periode fixe, active et eteinte seule aux dates prevues. */
    INTERIM
}
