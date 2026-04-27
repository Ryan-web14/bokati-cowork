package com.sni.bokaticowork.features.subscription.overage.dto;

import com.sni.bokaticowork.features.subscription.overage.enums.OveragePolicyMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOveragePolicyRequest(
        @NotNull Long planVersionId,
        @NotBlank String entitlementCode,
        @NotNull OveragePolicyMode mode,
        BigDecimal unitPrice,
        String currency,
        BigDecimal freeQuantity,
        String metadataJson,
        Boolean active
) {
}
