package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ActivityReportResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        BookingSection bookings,
        InvoiceSection invoices,
        PaymentSection payments,
        SubscriptionSection subscriptions,
        StockSection stock,
        ContractSection contracts,
        MemberSection members,
        SupportSection support
) {

    public record BookingSection(
            long totalCount,
            long confirmedCount,
            long completedCount,
            long cancelledCount,
            BigDecimal totalRevenue,
            long totalBookedMinutes,
            Variation variation
    ) {}

    public record InvoiceSection(
            long totalCount,
            BigDecimal totalInvoiced,
            BigDecimal totalPaid,
            BigDecimal totalOutstanding,
            long overdueCount,
            Variation variation
    ) {}

    public record PaymentSection(
            long transactionCount,
            BigDecimal totalReceived,
            BigDecimal cashAmount,
            BigDecimal walletAmount,
            BigDecimal mobileMoneyAmount,
            BigDecimal bankTransferAmount,
            BigDecimal cardAmount,
            Variation variation
    ) {}

    public record SubscriptionSection(
            long activeCount,
            long newCount,
            long cancelledCount,
            long renewedCount,
            Variation variation
    ) {}

    public record StockSection(
            long inboundMovements,
            long outboundMovements,
            BigDecimal inboundValue,
            BigDecimal outboundValue,
            Variation variation
    ) {}

    public record ContractSection(
            long signedCount,
            long expiredCount,
            long activeCount,
            Variation variation
    ) {}

    public record MemberSection(
            long totalActive,
            long newCount,
            Variation variation
    ) {}

    public record SupportSection(
            long openedCount,
            long resolvedCount,
            long closedCount,
            long highPriorityCount,
            Variation variation
    ) {}

    public record Variation(
            BigDecimal previousValue,
            BigDecimal currentValue,
            BigDecimal changePercent
    ) {}
}
