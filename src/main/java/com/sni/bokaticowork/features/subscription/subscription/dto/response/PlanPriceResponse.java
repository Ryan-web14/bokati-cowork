package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;

import java.math.BigDecimal;

public record PlanPriceResponse(
        Long id,
        BillingCycle billingCycle,
        String currency,
        BigDecimal amount,
        BigDecimal setupFee,
        BigDecimal depositAmount,
        Boolean taxIncluded,
        Integer trialDays,
        Integer commitmentMonths,
        String taxCode
) {
}
