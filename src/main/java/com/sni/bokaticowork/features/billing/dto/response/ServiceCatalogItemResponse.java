package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ServiceCatalogItemResponse(
        String itemCode,
        String name,
        String description,
        String category,
        String unit,
        BigDecimal unitPrice,
        String currency,
        String taxRuleCode,
        Boolean active,
        Integer displayOrder,
        Instant createdAt
) {
}
