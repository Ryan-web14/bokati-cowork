package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreatePaymentIntentRequest(
        @NotBlank String customerType,
        @NotBlank String customerCode,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        String purpose,
        String sourceType,
        String sourceCode,
        String idempotencyKey,
        Instant expiresAt,
        String metadataJson
) {
}
