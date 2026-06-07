package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateServiceCatalogItemRequest(
        @NotBlank String name,
        String description,
        String category,
        String unit,
        @NotNull BigDecimal unitPrice,
        String currency,
        String taxRuleCode,
        Integer displayOrder
) {
}
