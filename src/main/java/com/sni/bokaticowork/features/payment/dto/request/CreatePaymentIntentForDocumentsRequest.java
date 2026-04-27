package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public record CreatePaymentIntentForDocumentsRequest(
        @NotEmpty List<String> documentNumbers,
        String idempotencyKey,
        Instant expiresAt,
        String metadataJson
) {
}
