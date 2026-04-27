package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WalletLookupRequest(
        @NotBlank String ownerType,
        @NotBlank String ownerCode,
        @NotBlank String currency
) {
}
