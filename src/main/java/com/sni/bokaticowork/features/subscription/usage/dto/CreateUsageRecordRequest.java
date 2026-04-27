package com.sni.bokaticowork.features.subscription.usage.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateUsageRecordRequest(
        @NotNull SubscriberType ownerType,
        @NotBlank String ownerCode,
        @NotBlank String entitlementCode,
        @NotNull BigDecimal quantity,
        @NotNull EntitlementUnit unit,
        @NotBlank String referenceType,
        @NotBlank String referenceId,
        Boolean consumeEntitlement,
        Boolean billable,
        BigDecimal billableAmount,
        String currency,
        Instant occurredAt,
        String metadataJson
) {
}
