package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreatePaymentRecoveryIntentRequest(
        @NotBlank String customerType,
        @NotBlank String customerCode,
        String currency,
        Instant expiresAt,
        String metadataJson
) {
}
