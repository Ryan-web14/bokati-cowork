package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;

/**
 * Element en cours laisse par un titulaire : session, ecart en revue, demande, mouvement en attente.
 *
 * @param blocking vrai si l'element doit etre solde avant le transfert (une session ouverte)
 * @param fate     ce qu'il devient : {@code MUST_SETTLE} ou {@code TRANSFERRED} au nouveau titulaire
 */
public record CashPendingItemResponse(
        String kind,
        String reference,
        String label,
        BigDecimal amount,
        Boolean blocking,
        String fate
) {
}
