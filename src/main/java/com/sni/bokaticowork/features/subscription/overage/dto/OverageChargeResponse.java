package com.sni.bokaticowork.features.subscription.overage.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.Instant;

public record OverageChargeResponse(
        String chargeNumber,
        String subscriptionNumber,
        String usageNumber,
        String billableNumber,
        SubscriberType ownerType,
        String ownerCode,
        String entitlementCode,
        BigDecimal overageQuantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
}
