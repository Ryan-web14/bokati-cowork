package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreatePaymentIntentFromBillingDocumentRequest(
        @NotBlank String documentNumber,
        String idempotencyKey,
        Instant expiresAt,
        String metadataJson
) {
}
