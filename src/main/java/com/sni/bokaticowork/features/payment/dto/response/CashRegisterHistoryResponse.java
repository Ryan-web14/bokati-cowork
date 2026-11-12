package com.sni.bokaticowork.features.payment.dto.response;

import java.time.Instant;

/** Une ligne d'historique de la fiche d'une caisse. */
public record CashRegisterHistoryResponse(
        String field,
        String oldValue,
        String newValue,
        String changedBy,
        Instant changedAt,
        Instant effectiveAt,
        String reason
) {
}
