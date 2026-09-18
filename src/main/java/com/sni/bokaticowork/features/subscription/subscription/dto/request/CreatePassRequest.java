package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePassRequest(
        @NotNull PassType passType,
        @NotNull SubscriberType ownerType,
        @NotBlank String ownerCode,
        String subscriptionNumber,
        Long planVersionId,
        @NotBlank String name,
        String description,
        @NotNull String validFrom,
        String validUntil,
        Boolean transferable,
        Boolean shareable,
        Integer maxUses,
        String metadataJson,
        String idempotencyKey,
        @Valid List<PassEntitlementRequest> entitlements
) {
}
