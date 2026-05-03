package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegisterCashPaymentRequest(
        @NotBlank String receivedBy,
        @NotNull String cashSessionNumber,
        BigDecimal amount,
        String providerReference,
        String metadataJson
) {
}
