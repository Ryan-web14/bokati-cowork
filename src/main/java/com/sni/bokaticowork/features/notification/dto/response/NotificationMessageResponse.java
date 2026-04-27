package com.sni.bokaticowork.features.notification.dto.response;

import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;

import java.time.Instant;

public record NotificationMessageResponse(
        String notificationNumber,
        String eventType,
        String aggregateType,
        String aggregateId,
        NotificationChannel channel,
        NotificationRecipientType recipientType,
        String recipientCode,
        String recipientEmail,
        String recipientName,
        String subject,
        String templateCode,
        String templateName,
        String payloadJson,
        NotificationDeliveryStatus status,
        Integer attempts,
        Instant availableAt,
        Instant sentAt,
        String lastError,
        Instant createdAt
) {
}
