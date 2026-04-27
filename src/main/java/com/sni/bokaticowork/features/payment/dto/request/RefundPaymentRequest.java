package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record RefundPaymentRequest(
        BigDecimal amount,
        @NotBlank String reason,
        String processedBy
) {
}
