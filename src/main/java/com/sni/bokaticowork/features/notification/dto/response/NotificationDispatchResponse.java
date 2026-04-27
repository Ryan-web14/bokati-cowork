package com.sni.bokaticowork.features.notification.dto.response;

public record NotificationDispatchResponse(
        String notificationNumber,
        int webhookDeliveries
) {
}
