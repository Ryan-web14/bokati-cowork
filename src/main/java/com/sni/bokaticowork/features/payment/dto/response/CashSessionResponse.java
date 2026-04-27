package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CashSessionResponse(
        String sessionNumber,
        String registerCode,
        CashSessionStatus status,
        String openedBy,
        String closedBy,
        String reviewedBy,
        BigDecimal openingAmount,
        BigDecimal closingAmount,
        BigDecimal expectedClosingAmount,
        BigDecimal countedClosingAmount,
        BigDecimal varianceAmount,
        String varianceReason,
        Instant openedAt,
        Instant closingRequestedAt,
        Instant closedAt
) {
}
