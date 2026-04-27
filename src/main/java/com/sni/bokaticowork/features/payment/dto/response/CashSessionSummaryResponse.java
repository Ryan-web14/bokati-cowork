package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CashSessionSummaryResponse(
        String sessionNumber,
        String registerCode,
        CashSessionStatus status,
        String openedBy,
        String closedBy,
        BigDecimal openingAmount,
        BigDecimal totalPayments,
        BigDecimal totalRefunds,
        BigDecimal totalCashIn,
        BigDecimal totalCashOut,
        BigDecimal totalAdjustments,
        BigDecimal expectedClosingAmount,
        BigDecimal countedClosingAmount,
        BigDecimal varianceAmount,
        String varianceReason,
        Instant openedAt,
        Instant closingRequestedAt,
        Instant closedAt
) {
}
