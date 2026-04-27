package com.sni.bokaticowork.features.subscription.timeline.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;

import java.time.Instant;

public record SubscriptionTimelineEventResponse(
        String eventNumber,
        String subscriptionNumber,
        SubscriberType ownerType,
        String ownerCode,
        SubscriptionTimelineEventType eventType,
        String sourceType,
        String sourceId,
        String title,
        String description,
        String payloadJson,
        Instant occurredAt
) {
}
