package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateWalletHoldRequest(
        @NotBlank String walletNumber,
        @NotNull BigDecimal amount,
        String sourceType,
        String sourceCode,
        Instant expiresAt,
        String createdBy
) {
}
