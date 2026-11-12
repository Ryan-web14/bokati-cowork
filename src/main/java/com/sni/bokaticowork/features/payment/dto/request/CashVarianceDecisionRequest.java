package com.sni.bokaticowork.features.payment.dto.request;

import java.time.Instant;

/**
 * Decision sur un ecart. Sans decision explicite, la decision par defaut (charge pour un manquant,
 * produit pour un excedent) s'applique.
 *
 * @param reviewDueAt echeance de revue d'une mise en recherche ({@code INVESTIGATION})
 */
public record CashVarianceDecisionRequest(com.sni.bokaticowork.features.payment.enums.CashVarianceDecision decision, String note, Instant reviewDueAt) {
}
