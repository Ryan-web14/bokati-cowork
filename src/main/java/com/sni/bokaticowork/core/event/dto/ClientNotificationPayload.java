package com.sni.bokaticowork.core.event.dto;

import java.time.Instant;

public record ClientNotificationPayload(
        String notificationNumber,
        String title,
        String eventType,
        String aggregateType,
        String aggregateId,
        String payloadJson,
        Instant createdAt,
        boolean read
) {}
