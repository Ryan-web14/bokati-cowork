package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentIntentResponse(
        String intentNumber,
        String customerType,
        String customerCode,
        String customerName,
        String customerEmail,
        String customerPhone,
        String billingAddressJson,
        Boolean customerRegistered,
        BigDecimal amount,
        String currency,
        PaymentIntentStatus status,
        String purpose,
        String sourceType,
        String sourceCode,
        String resolvedSourceType,
        String resolvedSourceCode,
        String resolvedSourceLabel,
        Boolean resolvedSourceRegistered,
        String idempotencyKey,
        Instant expiresAt,
        String metadataJson
) {
}
