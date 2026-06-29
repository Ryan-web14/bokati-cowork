package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PassResponse(
        String passNumber,
        PassType passType,
        SubscriberType ownerType,
        String ownerCode,
        String subscriptionNumber,
        PassStatus status,
        String name,
        String description,
        Instant validFrom,
        Instant validUntil,
        Boolean transferable,
        Boolean shareable,
        Integer maxUses,
        Integer usedCount,
        String contractCode,
        String planCode,
        String planName,
        Integer planVersion,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Boolean autoRenew,
        Instant nextRenewalDate,
        Integer renewalCount,
        List<EntitlementGrantResponse> entitlements,
        Instant createdAt,
        Instant updatedAt
) {
}
