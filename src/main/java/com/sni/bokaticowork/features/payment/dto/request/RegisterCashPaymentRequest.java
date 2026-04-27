package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterCashPaymentRequest(
        @NotBlank String receivedBy,
        String cashSessionNumber,
        String providerReference,
        String metadataJson
) {
}
