package com.sni.bokaticowork.features.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AnalyticsOverviewResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        FinancialMetrics financial,
        PaymentMetrics payment,
        WalletMetrics wallet,
        BookingMetrics booking,
        SubscriptionMetrics subscription,
        CustomerMetrics customer,
        InventoryMetrics inventory
) {

    public record FinancialMetrics(
            long invoiceCount,
            BigDecimal invoicedAmount,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            long overdueInvoiceCount,
            long quotationCount,
            BigDecimal quotedAmount,
            BigDecimal vatAmount,
            BigDecimal additionalCentAmount
    ) {
    }

    public record PaymentMetrics(
            long transactionCount,
            BigDecimal succeededAmount,
            BigDecimal cashAmount,
            BigDecimal walletAmount,
            BigDecimal mobileMoneyAmount,
            BigDecimal bankTransferAmount,
            BigDecimal cardAmount,
            long failedTransactionCount,
            long pendingTransactionCount
    ) {
    }

    public record WalletMetrics(
            long walletCount,
            BigDecimal availableBalance,
            BigDecimal heldBalance,
            BigDecimal credits,
            BigDecimal debits
    ) {
    }

    public record BookingMetrics(
            long bookingCount,
            long confirmedBookings,
            long completedBookings,
            long cancelledBookings,
            long noShowBookings,
            BigDecimal bookingRevenue,
            long bookedMinutes,
            long participantCount
    ) {
    }

    public record SubscriptionMetrics(
            long activeSubscriptions,
            long pendingSubscriptions,
            long suspendedSubscriptions,
            long activePasses,
            BigDecimal monthlyRecurringRevenue,
            long billableUsageQuantity
    ) {
    }

    public record CustomerMetrics(
            long customerCount,
            long memberCount,
            long activeMembers,
            long guestBookingCount
    ) {
    }

    public record InventoryMetrics(
            long itemCount,
            BigDecimal stockValue,
            long lowStockAlerts,
            long openPurchaseOrders
    ) {
    }
}
