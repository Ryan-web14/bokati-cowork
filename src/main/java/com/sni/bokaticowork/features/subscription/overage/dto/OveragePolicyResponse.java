package com.sni.bokaticowork.features.subscription.overage.dto;

import com.sni.bokaticowork.features.subscription.overage.enums.OveragePolicyMode;

import java.math.BigDecimal;

public record OveragePolicyResponse(
        Long id,
        Long planVersionId,
        String planCode,
        String entitlementCode,
        OveragePolicyMode mode,
        BigDecimal unitPrice,
        String currency,
        BigDecimal freeQuantity,
        String metadataJson,
        Boolean active
) {
}
