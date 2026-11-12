package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.Valid;

/**
 * Reponse de l'entrant : accepter le comptage du sortant, ou le contester avec son propre comptage
 * et un motif. L'entrant envoie aussi son poste.
 */
public record CashHandoverAnswerRequest(@Valid CashCountRequest count, String reason, String deviceCode) {
}
