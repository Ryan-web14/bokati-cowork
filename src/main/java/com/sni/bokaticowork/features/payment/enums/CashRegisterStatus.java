package com.sni.bokaticowork.features.payment.enums;

/** Cycle de vie d'une caisse. */
public enum CashRegisterStatus {
    /** Utilisable. */
    ACTIVE,
    /** Bloquee temporairement (controle, enquete) ; revient en ACTIVE ou passe en CLOSED. */
    SUSPENDED,
    /** Fermee definitivement ; reste consultable, son code ne se reutilise pas. */
    CLOSED
}
