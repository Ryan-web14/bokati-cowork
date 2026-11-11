package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;

/**
 * Apercu d'un encaissement en especes, avant de valider.
 *
 * @param amountDue       reste a payer
 * @param tenderedAmount  ce que le client tend
 * @param appliedAmount   ce qui sera impute et entrera en caisse
 * @param changeAmount    la monnaie a rendre
 * @param remainingAfter  ce qui restera du apres l'encaissement (paiement partiel)
 */
public record CashChangeResponse(
        String currency,
        BigDecimal amountDue,
        BigDecimal tenderedAmount,
        BigDecimal appliedAmount,
        BigDecimal changeAmount,
        BigDecimal remainingAfter
) {
}
