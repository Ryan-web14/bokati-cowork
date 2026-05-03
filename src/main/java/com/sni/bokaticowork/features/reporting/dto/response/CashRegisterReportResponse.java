package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CashRegisterReportResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalCashIn,
        BigDecimal totalCashOut,
        BigDecimal netFlow,
        List<RegisterSummary> registers
) {

    public record RegisterSummary(
            String registerCode,
            String registerName,
            long sessionsCount,
            BigDecimal openingAmount,
            BigDecimal cashIn,
            BigDecimal cashOut,
            BigDecimal netFlow,
            BigDecimal totalVariance,
            long movementsCount
    ) {}
}