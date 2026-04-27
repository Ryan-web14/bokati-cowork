package com.sni.bokaticowork.features.notification.dto.response;

import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;

import java.time.Instant;

public record WebhookDeliveryResponse(
        String deliveryNumber,
        String endpointCode,
        String endpointName,
        String eventType,
        String aggregateType,
        String aggregateId,
        String payloadJson,
        NotificationDeliveryStatus status,
        Integer attempts,
        Instant availableAt,
        Instant deliveredAt,
        Integer httpStatus,
        String lastError,
        Instant createdAt
) {
}
