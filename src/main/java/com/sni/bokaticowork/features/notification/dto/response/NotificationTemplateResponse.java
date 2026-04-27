package com.sni.bokaticowork.features.notification.dto.response;

import com.sni.bokaticowork.features.notification.enums.NotificationChannel;

import java.time.Instant;

public record NotificationTemplateResponse(
        String templateCode,
        NotificationChannel channel,
        String subject,
        String templateName,
        String bodyTemplate,
        Boolean active,
        Boolean adminOnly,
        Instant createdAt,
        Instant updatedAt
) {
}
