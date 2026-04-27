package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateBillingDocumentDiscountRequest(
        String discountCode,
        @NotBlank String description,
        @NotNull BillingDiscountType discountType,
        @NotNull BigDecimal value
) {
}
