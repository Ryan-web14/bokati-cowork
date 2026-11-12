package com.sni.bokaticowork.features.payment.dto.response;

import java.util.List;

/**
 * Resultat d'une etape de session (ouverture, comptage, decision...) : la session, le comptage
 * enregistre s'il y en a un, l'ecart cree s'il y en a un, et des messages a afficher.
 */
public record CashSessionStepResponse(
        CashSessionResponse session,
        CashCountResponse count,
        CashVarianceReviewResponse review,
        List<String> messages
) {
}
