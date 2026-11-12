package com.sni.bokaticowork.core.accounting;

import java.time.LocalDate;

/**
 * Evenements des abonnements qui deplacent la reconnaissance du chiffre d'affaires (lot F20), sans
 * dependre de la comptabilite. Sans implementation, rien ne change.
 */
public interface DeferralEvents {

    /** Gel d'un abonnement : ses echeanciers en cours reculent de la duree prevue. */
    void subscriptionFrozen(String subscriptionNumber, LocalDate from, int plannedDays);

    /** Reprise : la duree reellement gelee remplace la duree prevue. */
    void subscriptionResumed(String subscriptionNumber, LocalDate from, int effectiveDays);
}
