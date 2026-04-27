package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;

import java.time.Instant;
import java.time.LocalDate;

public record BillingScheduleResponse(
        String subscriptionNumber,
        BillingCycle billingCycle,
        LocalDate nextBillingDate,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        BillingScheduleStatus status,
        Integer retryCount,
        Instant lastAttemptAt
) {
}
