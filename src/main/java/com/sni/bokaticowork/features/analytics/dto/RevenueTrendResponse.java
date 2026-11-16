package com.sni.bokaticowork.features.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Chiffre d'affaires d'une periode (jour, semaine commencant le lundi, ou mois).
 *
 * @param date            premier jour de la periode
 * @param invoiceCount    factures emises (memes regles que le resume financier : ni brouillon, ni
 *                        rejetee, expiree, convertie, annulee)
 * @param invoicedAmount  montant TTC de ces factures
 * @param paymentCount    paiements reussis
 * @param collectedAmount montant encaisse par ces paiements
 */
public record RevenueTrendResponse(
        LocalDate date,
        long invoiceCount,
        BigDecimal invoicedAmount,
        long paymentCount,
        BigDecimal collectedAmount
) {
}
