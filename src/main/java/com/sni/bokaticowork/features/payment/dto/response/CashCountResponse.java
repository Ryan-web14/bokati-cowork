package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Un comptage ; l'attendu et l'ecart ne sont remplis qu'une fois le comptage valide. */
public record CashCountResponse(
        String countNumber,
        String registerCode,
        String sessionNumber,
        com.sni.bokaticowork.features.payment.enums.CashCountMoment moment,
        String currency,
        BigDecimal total,
        BigDecimal damagedTotal,
        Boolean withLines,
        BigDecimal expectedAmount,
        BigDecimal variance,
        String countedBy,
        String secondCounter,
        String witness,
        String status,
        String supersededBy,
        String referenceNumber,
        String note,
        Instant createdAt,
        List<Line> lines
) {
    public record Line(BigDecimal value, com.sni.bokaticowork.features.payment.enums.CashDenominationKind kind, Integer quantity, Integer damagedQuantity, BigDecimal amount) {
    }
}
