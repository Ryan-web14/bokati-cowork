package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record BillingAgingReportResponse(
        Instant generatedAt,
        BigDecimal totalOutstanding,
        long totalInvoices,
        AgingBucket current,
        AgingBucket days1to30,
        AgingBucket days31to60,
        AgingBucket days61to90,
        AgingBucket over90
) {

    public record AgingBucket(
            String label,
            long count,
            BigDecimal amount,
            BigDecimal percentage
    ) {}
}