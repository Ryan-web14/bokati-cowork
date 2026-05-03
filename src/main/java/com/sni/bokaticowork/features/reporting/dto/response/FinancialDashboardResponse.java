package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FinancialDashboardResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,

        long invoiceCount,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalOutstanding,
        long overdueCount,
        BigDecimal overdueAmount,
        BigDecimal collectionRate,
        BigDecimal totalVat,
        BigDecimal totalAdditionalCent,
        BigDecimal totalDiscounts,
        long cancelledInvoiceCount,

        List<RevenueBySource> revenueBySource,
        PaymentSummary payments
) {

    public record RevenueBySource(
            String sourceType,
            long invoiceCount,
            BigDecimal totalInvoiced,
            BigDecimal totalPaid,
            BigDecimal outstandingAmount
    ) {}

    public record PaymentSummary(
            long transactionCount,
            BigDecimal totalReceived,
            BigDecimal cashAmount,
            BigDecimal walletAmount,
            BigDecimal mobileMoneyAmount,
            BigDecimal bankTransferAmount,
            BigDecimal cardAmount,
            BigDecimal chequeAmount,
            long failedCount,
            long pendingCount
    ) {}
}