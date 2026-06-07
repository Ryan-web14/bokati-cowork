package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewCashRequestRequest(
        @NotBlank String reviewedBy,
        String note
) {
}
