package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ApproveCashVarianceRequest(
        @NotBlank String reviewedBy,
        String note
) {
}
