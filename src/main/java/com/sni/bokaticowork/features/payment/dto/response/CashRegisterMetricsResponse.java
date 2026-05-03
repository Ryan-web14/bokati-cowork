package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;

public record CashRegisterMetricsResponse(
        String registerCode,
        String registerName,
        String businessEntityCode,
        String deviceCode,
        long openSessionCount,
        long closingReviewSessionCount,
        long movementCount,
        BigDecimal totalPayments,
        BigDecimal totalRefunds,
        BigDecimal totalCashIn,
        BigDecimal totalCashOut,
        BigDecimal totalAdjustments,
        BigDecimal netCashPosition
) {
}
