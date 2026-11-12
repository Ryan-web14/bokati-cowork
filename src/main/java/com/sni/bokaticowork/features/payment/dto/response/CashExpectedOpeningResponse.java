package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Ce qu'une caisse doit contenir a l'ouverture : son dernier comptage. */
public record CashExpectedOpeningResponse(
        String registerCode,
        BigDecimal expectedAmount,
        Instant lastCountAt,
        Boolean neverCounted,
        BigDecimal standardFloat,
        Boolean countLinesRequired,
        List<CashDenominationResponse> denominations
) {
}
