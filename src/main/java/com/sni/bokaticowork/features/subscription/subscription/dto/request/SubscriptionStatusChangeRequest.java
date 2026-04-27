package com.sni.bokaticowork.features.subscription.subscription.dto.request;

public record SubscriptionStatusChangeRequest(
        String reason,
        String changedBy,
        Boolean cancelAtPeriodEnd
) {
}
