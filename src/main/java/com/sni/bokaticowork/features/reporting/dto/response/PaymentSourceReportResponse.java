package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PaymentSourceReportResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalReceived,
        long totalTransactions,
        List<MethodBreakdown> byMethod,
        List<DailyPaymentEntry> dailySeries
) {

    public record MethodBreakdown(
            String method,
            long count,
            BigDecimal amount,
            BigDecimal percentage
    ) {}

    public record DailyPaymentEntry(
            LocalDate date,
            BigDecimal cash,
            BigDecimal wallet,
            BigDecimal mobileMoney,
            BigDecimal bankTransfer,
            BigDecimal card,
            BigDecimal cheque,
            BigDecimal total
    ) {}
}