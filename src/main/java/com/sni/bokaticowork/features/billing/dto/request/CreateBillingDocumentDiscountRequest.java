package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateBillingDocumentDiscountRequest(
        String discountCode,
        @NotBlank String description,
        @NotNull BillingDiscountType discountType,
        @NotNull BigDecimal value,
        /** Origine de la remise. Nulle pour une remise saisie a la main. */
        String sourceType,
        String sourceCode
) {

    /** Remise saisie a la main, sans campagne derriere elle. */
    public CreateBillingDocumentDiscountRequest(String discountCode, String description,
                                                BillingDiscountType discountType, BigDecimal value) {
        this(discountCode, description, discountType, value, null, null);
    }
}
