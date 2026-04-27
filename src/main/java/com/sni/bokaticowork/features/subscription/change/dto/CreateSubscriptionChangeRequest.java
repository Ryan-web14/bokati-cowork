package com.sni.bokaticowork.features.subscription.change.dto;

import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeEffectivePolicy;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubscriptionChangeRequest(
        @NotNull SubscriptionChangeType changeType,
        Long targetPlanVersionId,
        @NotNull SubscriptionChangeEffectivePolicy effectivePolicy,
        LocalDate effectiveDate,
        BigDecimal prorationAmount,
        String reason,
        String requestedBy
) {
}
