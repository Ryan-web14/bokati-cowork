package com.sni.bokaticowork.features.subscription.change.dto;

import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeEffectivePolicy;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeStatus;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionChangeResponse(
        String changeNumber,
        String subscriptionNumber,
        SubscriptionChangeType changeType,
        Long currentPlanVersionId,
        Long targetPlanVersionId,
        SubscriptionChangeEffectivePolicy effectivePolicy,
        LocalDate effectiveDate,
        BigDecimal prorationAmount,
        com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy prorationPolicy,
        Boolean prorationCredit,
        String prorationBillableNumber,
        SubscriptionChangeStatus status,
        String reason,
        String requestedBy,
        String approvedBy,
        Instant appliedAt
) {
}
