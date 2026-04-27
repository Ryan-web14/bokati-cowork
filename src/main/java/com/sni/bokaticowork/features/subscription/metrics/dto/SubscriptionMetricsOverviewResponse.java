package com.sni.bokaticowork.features.subscription.metrics.dto;

import java.math.BigDecimal;

public record SubscriptionMetricsOverviewResponse(
        long activeSubscriptions,
        long pendingSubscriptions,
        long suspendedSubscriptions,
        long cancelledSubscriptions,
        long activePasses,
        long activeAddons,
        long activePromotions,
        long pendingBillableItems,
        BigDecimal pendingBillableAmount,
        BigDecimal activeSubscriptionMrr,
        BigDecimal recordedUsageQuantity,
        BigDecimal billableUsageQuantity,
        long overageCharges,
        BigDecimal overageAmount,
        long rolloverRecords,
        BigDecimal rolloverQuantity
) {
}
