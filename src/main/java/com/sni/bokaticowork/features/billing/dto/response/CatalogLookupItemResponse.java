package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record CatalogLookupItemResponse(
        String sourceType,
        String sourceCode,
        String name,
        String description,
        String category,
        String unit,
        BigDecimal unitPrice,
        String currency,
        String taxRuleCode,
        Map<String, Object> meta
) {
}
