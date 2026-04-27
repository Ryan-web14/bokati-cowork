package com.sni.bokaticowork.features.subscription.timeline.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateTimelineEventRequest(
        String subscriptionNumber,
        SubscriberType ownerType,
        String ownerCode,
        @NotNull SubscriptionTimelineEventType eventType,
        String sourceType,
        String sourceId,
        @NotBlank String title,
        String description,
        String payloadJson,
        Instant occurredAt
) {
}
