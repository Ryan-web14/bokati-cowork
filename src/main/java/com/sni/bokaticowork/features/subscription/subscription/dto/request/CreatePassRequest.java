package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreatePassRequest(
        @NotNull PassType passType,
        @NotNull SubscriberType ownerType,
        @NotBlank String ownerCode,
        String subscriptionNumber,
        Long planVersionId,
        @NotBlank String name,
        String description,
        @NotNull Instant validFrom,
        Instant validUntil,
        Boolean transferable,
        Boolean shareable,
        Integer maxUses,
        String metadataJson,
        @Valid List<PassEntitlementRequest> entitlements
) {
}
