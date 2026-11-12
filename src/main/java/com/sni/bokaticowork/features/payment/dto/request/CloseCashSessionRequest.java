package com.sni.bokaticowork.features.payment.dto.request;


import java.math.BigDecimal;

public record CloseCashSessionRequest(
        /** Ignore en contexte authentifie : l'auteur est l'utilisateur connecte. */
        String closedBy,
        BigDecimal closingAmount,
        BigDecimal countedClosingAmount,
        String varianceReason
) {
}
