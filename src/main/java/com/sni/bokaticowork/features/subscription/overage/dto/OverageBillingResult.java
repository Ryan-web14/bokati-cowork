package com.sni.bokaticowork.features.subscription.overage.dto;

import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;

import java.math.BigDecimal;

public record OverageBillingResult(
        boolean handled,
        boolean billable,
        BigDecimal consumedQuantity,
        BigDecimal overageQuantity,
        BillableItem billableItem,
        Subscription subscription
) {
    public static OverageBillingResult none() {
        return new OverageBillingResult(false, false, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }
}
