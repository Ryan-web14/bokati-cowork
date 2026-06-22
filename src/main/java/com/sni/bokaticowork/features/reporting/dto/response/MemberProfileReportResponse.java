package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MemberProfileReportResponse(
        Instant generatedAt,
        MemberInfo member,
        List<SubscriptionSummary> subscriptions,
        List<BookingSummary> bookings,
        List<InvoiceSummary> invoices,
        List<PaymentSummary> payments,
        WalletInfo wallet,
        List<ContractSummary> contracts,
        List<TicketSummary> tickets
) {

    public record MemberInfo(
            String memberCode,
            String firstname,
            String lastname,
            String email,
            String phone,
            String whatsappPhone,
            String status,
            String customerCode,
            String customerName,
            String jobTitle,
            String companyRole,
            LocalDate birthDate,
            String city,
            String country,
            String photoUrl,
            boolean portalAccess,
            LocalDate createdAt
    ) {}

    public record SubscriptionSummary(
            String subscriptionNumber,
            String planName,
            String status,
            String billingCycle,
            BigDecimal totalAmount,
            String currency,
            LocalDate startDate,
            LocalDate currentPeriodEnd,
            boolean autoRenew
    ) {}

    public record BookingSummary(
            String bookingNumber,
            String resourceName,
            String status,
            LocalDate startDate,
            int durationMinutes,
            BigDecimal totalAmount,
            String currency
    ) {}

    public record InvoiceSummary(
            String documentNumber,
            String status,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal balanceDue,
            String currency,
            LocalDate issueDate,
            LocalDate dueDate
    ) {}

    public record PaymentSummary(
            String transactionNumber,
            String paymentMethod,
            BigDecimal amount,
            String currency,
            String status,
            LocalDate paidAt
    ) {}

    public record WalletInfo(
            String walletNumber,
            BigDecimal availableBalance,
            BigDecimal ledgerBalance,
            BigDecimal heldBalance,
            String currency,
            String status
    ) {}

    public record ContractSummary(
            String contractCode,
            String title,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            String renewalType,
            LocalDate signedAt
    ) {}

    public record TicketSummary(
            String ticketNumber,
            String title,
            String status,
            String priority,
            String category,
            LocalDate createdAt,
            LocalDate resolvedAt
    ) {}
}
