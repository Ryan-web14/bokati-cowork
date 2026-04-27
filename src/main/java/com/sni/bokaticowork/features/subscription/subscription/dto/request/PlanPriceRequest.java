package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlanPriceRequest(
        @NotNull BillingCycle billingCycle,
        @NotBlank String currency,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        BigDecimal setupFee,
        BigDecimal depositAmount,
        Boolean taxIncluded,
        Integer commitmentMonths
) {
}
