package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionHistoryResponse(
        SubscriptionStatus fromStatus,
        SubscriptionStatus toStatus,
        String reason,
        String changedBy,
        Instant changedAt
) {
}
