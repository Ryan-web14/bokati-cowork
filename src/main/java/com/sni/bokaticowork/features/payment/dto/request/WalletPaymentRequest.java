package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record WalletPaymentRequest(
        @NotBlank String walletNumber,
        BigDecimal amount,
        String createdBy,
        String metadataJson
) {
}
