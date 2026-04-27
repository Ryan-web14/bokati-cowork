package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WalletPaymentRequest(
        @NotBlank String walletNumber,
        String createdBy,
        String metadataJson
) {
}
