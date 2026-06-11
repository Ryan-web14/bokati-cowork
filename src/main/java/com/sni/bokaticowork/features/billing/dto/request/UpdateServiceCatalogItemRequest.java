package com.sni.bokaticowork.features.billing.dto.request;

import java.math.BigDecimal;

public record UpdateServiceCatalogItemRequest(
        String name,
        String description,
        String category,
        String unit,
        BigDecimal unitPrice,
        String currency,
        String taxRuleCode,
        Integer displayOrder
) {
}
