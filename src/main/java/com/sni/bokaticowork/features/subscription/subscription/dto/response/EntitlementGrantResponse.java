package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.Instant;

public record EntitlementGrantResponse(
        String grantNumber,
        String entitlementCode,
        String entitlementName,
        EntitlementUnit unit,
        SubscriberType ownerType,
        String ownerCode,
        BigDecimal quantityGranted,
        BigDecimal quantityRemaining,
        Boolean unlimited,
        Instant validFrom,
        Instant validUntil,
        EntitlementGrantStatus status,
        String sourceType,
        String sourceId
) {
}
