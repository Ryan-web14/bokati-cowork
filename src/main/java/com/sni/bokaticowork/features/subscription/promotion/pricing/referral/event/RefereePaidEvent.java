package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.event;

/**
 * Un client vient de regler un paiement. S'il a ete parraine sur un programme qui recompense au
 * premier paiement, son parrainage se qualifie.
 *
 * @param customerType le type du payeur ({@code MEMBER}, {@code CUSTOMER}...), comme sur l'intention de paiement
 * @param customerCode son code
 * @param transactionNumber le paiement, pour les journaux
 */
public record RefereePaidEvent(String customerType, String customerCode, String transactionNumber) {
}
