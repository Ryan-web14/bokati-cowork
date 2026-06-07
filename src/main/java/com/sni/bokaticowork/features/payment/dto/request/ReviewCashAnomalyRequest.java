package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.CashAnomalyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewCashAnomalyRequest(
        @NotBlank String reviewedBy,
        @NotNull CashAnomalyStatus status,
        String note
) {
}
