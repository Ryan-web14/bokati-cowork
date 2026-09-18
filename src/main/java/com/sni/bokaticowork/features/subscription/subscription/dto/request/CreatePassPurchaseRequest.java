package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePassPurchaseRequest(
        @NotNull SubscriberType ownerType,
        @NotBlank String ownerCode,
        Long planVersionId,
        String currency,
        String validFrom,
        Boolean autoRenew,
        String metadataJson,
        String idempotencyKey
) {}
