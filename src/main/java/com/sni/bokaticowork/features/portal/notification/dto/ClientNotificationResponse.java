package com.sni.bokaticowork.features.portal.notification.dto;

import java.time.Instant;

public record ClientNotificationResponse(
        String notificationNumber,
        String title,
        String message,
        Instant createdAt,
        boolean read
) {
}
