package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record OpenCashSessionRequest(
        @NotBlank String registerCode,
        @NotBlank String openedBy,
        BigDecimal openingAmount
) {
}
