package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EntitlementOperationRequest(
        @NotNull SubscriberType ownerType,
        @NotBlank String ownerCode,
        @NotBlank String entitlementCode,
        @NotNull BigDecimal quantity,
        String referenceType,
        String referenceId,
        String idempotencyKey,
        String reason
) {
}
