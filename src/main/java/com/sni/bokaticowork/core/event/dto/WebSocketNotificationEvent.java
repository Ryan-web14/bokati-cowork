package com.sni.bokaticowork.core.event.dto;

import java.time.Instant;

public record WebSocketNotificationEvent(
        String notificationNumber,
        String recipientEmail,
        String eventType,
        String aggregateType,
        String aggregateId,
        String subject,
        String payloadJson,
        Instant createdAt
) {}
