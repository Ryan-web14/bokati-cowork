package com.sni.bokaticowork.features.payment.enums;

/** Comportement au plafond de detention. */
public enum CashCapBehavior {
    /** Alerte au responsable, l'operation passe. */
    ALERT,
    /** L'encaissement en especes est refuse jusqu'a la remise. */
    BLOCK
}
