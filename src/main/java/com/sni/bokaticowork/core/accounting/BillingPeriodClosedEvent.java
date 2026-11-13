package com.sni.bokaticowork.core.accounting;

import java.time.LocalDate;

/**
 * Une periode de facturation vient d'etre close (scellement des totaux). La comptabilite peut
 * proposer, ou faire, le verrouillage de la periode comptable correspondante (plan, section 4.2).
 *
 * @param periodType {@code DAY}, {@code MONTH} ou {@code YEAR}
 */
public record BillingPeriodClosedEvent(String periodType, String periodLabel, LocalDate periodStart, LocalDate periodEnd) {
}
