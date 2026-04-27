package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;

import java.math.BigDecimal;

public record BillingDocumentDiscountResponse(
        String discountCode,
        String description,
        BillingDiscountType discountType,
        BigDecimal value,
        BigDecimal amount
) {
}
