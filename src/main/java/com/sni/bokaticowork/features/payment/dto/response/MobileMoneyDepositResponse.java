package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MobileMoneyDepositResponse(
        String depositId,
        String clientReferenceId,
        String customerMessage,
        List<Map<String, Object>> metadata,
        PayerResponse payer,
        String provider,
        BigDecimal amount,
        String currency,
        String status,
        String providerMessage,
        String failureReason,
        String intentNumber,
        String transactionNumber,
        String customerType,
        String customerCode,
        String callbackUrl,
        String requestPayloadJson,
        String providerResponseJson,
        Instant lastStatusCheckedAt,
        Instant completedAt,
        Instant failedAt,
        Instant createdAt,
        Instant updatedAt,
        PaymentTransactionResponse transaction
) {
    public record PayerResponse(
            String type,
            AccountDetailsResponse accountDetails
    ) {
    }

    public record AccountDetailsResponse(
            String phoneNumber,
            String provider
    ) {
    }
}
