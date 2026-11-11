package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CashSessionSummaryResponse(
        String sessionNumber,
        String registerCode,
        CashSessionStatus status,
        String openedBy,
        String closedBy,
        BigDecimal openingAmount,
        BigDecimal totalPayments,
        BigDecimal totalRefunds,
        BigDecimal totalCashIn,
        BigDecimal totalCashOut,
        BigDecimal totalAdjustments,
        BigDecimal expectedClosingAmount,
        BigDecimal countedClosingAmount,
        BigDecimal varianceAmount,
        String varianceReason,
        Instant openedAt,
        Instant closingRequestedAt,
        Instant closedAt,
        /**
         * Paiements saisis a ce guichet qui ne sont pas des especes (cheque, virement, mobile money
         * hors plateforme...). Hors du compte des especes : ni dans {@code totalPayments} ni dans
         * {@code expectedClosingAmount}.
         */
        java.util.List<NonCashPaymentTotal> nonCashPayments
) {

    public record NonCashPaymentTotal(
            com.sni.bokaticowork.features.payment.enums.PaymentMethod paymentMethod,
            String provider,
            String label,
            long count,
            BigDecimal amount
    ) {
    }
}
