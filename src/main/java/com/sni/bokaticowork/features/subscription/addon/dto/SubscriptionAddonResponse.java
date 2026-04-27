package com.sni.bokaticowork.features.subscription.addon.dto;

import com.sni.bokaticowork.features.subscription.addon.enums.SubscriptionAddonStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionAddonResponse(
        Long id,
        String subscriptionNumber,
        Long planVersionId,
        String planCode,
        String planName,
        Integer quantity,
        BigDecimal unitPrice,
        String currency,
        SubscriptionAddonStatus status,
        LocalDate startsAt,
        LocalDate endsAt,
        String contractCode,
        String metadataJson
) {
}
