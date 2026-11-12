package com.sni.bokaticowork.features.payment.enums;

/** Comportement de fin de mois d'une caisse manuelle. */
public enum CashMonthEndPolicy {
    /** La session encore ouverte est suspendue ; un superviseur la compte et la clot. */
    REQUIRE_COUNT,
    /** Ancien comportement : cloture sans comptage et report, signale. */
    CARRY_OVER
}
