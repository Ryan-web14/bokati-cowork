package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CashFlowReportResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalInflows,
        BigDecimal totalOutflows,
        BigDecimal netCashFlow,
        List<CashFlowByType> byMovementType,
        List<DailyCashFlowEntry> dailySeries
) {

    public record CashFlowByType(
            String movementType,
            String direction,
            long count,
            BigDecimal totalAmount
    ) {}

    public record DailyCashFlowEntry(
            LocalDate date,
            BigDecimal inflows,
            BigDecimal outflows,
            BigDecimal net
    ) {}
}