package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CloseCashSessionRequest(
        @NotBlank String closedBy,
        BigDecimal closingAmount,
        BigDecimal countedClosingAmount,
        String varianceReason
) {
}
