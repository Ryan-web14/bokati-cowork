package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record RecoverableItemRequest(
        @NotBlank String itemDescription,
        BigDecimal quantity,
        String unit,
        String sourceType,
        String sourceCode,
        String notes
) {
}
