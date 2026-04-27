package com.sni.bokaticowork.features.notification.dto.response;

import java.time.Instant;
import java.util.Set;

public record WebhookEndpointResponse(
        String endpointCode,
        String name,
        String url,
        Set<String> eventTypes,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
