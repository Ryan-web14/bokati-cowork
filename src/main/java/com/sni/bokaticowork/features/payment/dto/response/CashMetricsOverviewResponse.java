package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;

public record CashMetricsOverviewResponse(
        String currency,
        long registerCount,
        long activeRegisterCount,
        long openSessionCount,
        long closingReviewSessionCount,
        long closedSessionCount,
        long movementCount,
        BigDecimal openingFloatAmount,
        BigDecimal totalPayments,
        BigDecimal totalRefunds,
        BigDecimal totalCashIn,
        BigDecimal totalCashOut,
        BigDecimal totalAdjustments,
        BigDecimal netCashPosition,
        BigDecimal pendingVarianceAmount
) {
}
