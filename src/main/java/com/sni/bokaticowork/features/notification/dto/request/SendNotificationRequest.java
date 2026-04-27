package com.sni.bokaticowork.features.notification.dto.request;

import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record SendNotificationRequest(
        @NotBlank String eventType,
        String aggregateType,
        String aggregateId,
        @NotNull NotificationChannel channel,
        NotificationRecipientType recipientType,
        String recipientCode,
        String recipientEmail,
        String recipientName,
        String subject,
        String templateCode,
        String templateName,
        Map<String, Object> payload,
        Instant availableAt
) {
}
