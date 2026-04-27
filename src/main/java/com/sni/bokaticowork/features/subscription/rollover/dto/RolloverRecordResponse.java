package com.sni.bokaticowork.features.subscription.rollover.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RolloverRecordResponse(
        String rolloverNumber,
        String subscriptionNumber,
        String sourceGrantNumber,
        String rolloverGrantNumber,
        String entitlementCode,
        BigDecimal quantity,
        Instant createdAt
) {
}
