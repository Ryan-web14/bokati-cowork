package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateReconciliationBatchRequest(
        @NotBlank String provider,
        String createdBy
) {
}
