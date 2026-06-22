package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DebtRecoveryReportResponse(
        Instant generatedAt,
        BigDecimal totalOutstanding,
        long totalInvoices,
        AgingSummary aging,
        List<UnpaidInvoice> unpaidInvoices
) {

    public record AgingSummary(
            AgingBucket current,
            AgingBucket days1to30,
            AgingBucket days31to60,
            AgingBucket days61to90,
            AgingBucket days90plus
    ) {}

    public record AgingBucket(
            String label,
            long invoiceCount,
            BigDecimal totalAmount,
            BigDecimal averageAmount,
            BigDecimal percentage
    ) {}

    public record UnpaidInvoice(
            String documentNumber,
            String customerName,
            String customerCode,
            String customerType,
            String customerEmail,
            String customerPhone,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal balanceDue,
            long agingDays,
            String agingBucket
    ) {}
}
