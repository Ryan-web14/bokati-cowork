package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

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
        List<EntitlementGrantResponse> entitlements
) {
}
