package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WalletTopUpRequest(
        @NotBlank String ownerType,
        @NotBlank String ownerCode,
        @NotBlank String currency,
        @NotNull BigDecimal amount,
        /** Ignore pour un compte connecte : l'auteur est le compte du jeton. */
        String createdBy,
        String reference,
        String metadataJson
) {
}
