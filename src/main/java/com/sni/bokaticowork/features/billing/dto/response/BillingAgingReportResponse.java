package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BillingAgingReportResponse(
        Instant generatedAt,
        String currency,
        AgingSummary summary,
        List<AgingDocument> documents
) {
    public record AgingBucket(int count, BigDecimal totalDue) {}

    public record AgingSummary(
            AgingBucket current,
            AgingBucket days1To30,
            AgingBucket days31To60,
            AgingBucket days61To90,
            AgingBucket daysOver90,
            AgingBucket grandTotal
    ) {}

    public record AgingDocument(
            String documentNumber,
            String customerType,
            String customerCode,
            String customerName,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal balanceDue,
            long daysOverdue,
            String agingBucket,
            String status
    ) {}
}
