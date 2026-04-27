package com.sni.bokaticowork.features.subscription.usage.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.usage.enums.UsageRecordStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record UsageRecordResponse(
        String usageNumber,
        SubscriberType ownerType,
        String ownerCode,
        String entitlementCode,
        BigDecimal quantity,
        EntitlementUnit unit,
        String referenceType,
        String referenceId,
        Boolean billable,
        String billableNumber,
        UsageRecordStatus status,
        Instant occurredAt,
        String metadataJson
) {
}
