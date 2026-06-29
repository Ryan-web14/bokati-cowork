package com.sni.bokaticowork.core.communication.mailService.dto;

import java.io.Serializable;
import java.time.Instant;

public record NotificationRabbitMessage(
        String notificationNumber,
        String channel,
        String recipientEmail,
        String recipientName,
        String eventType,
        String aggregateType,
        String aggregateId,
        String subject,
        String payloadJson,
        String templateCode,
        String templateName,
        Instant createdAt
) implements Serializable {

    public String routingKey() {
        return switch (channel) {
            case "IN_APP" -> "notification.inapp." + safeEventType();
            case "WEBHOOK" -> "notification.webhook." + safeEventType();
            default -> "notification.admin." + safeEventType();
        };
    }

    private String safeEventType() {
        return eventType != null ? eventType.toLowerCase().replace("_", ".") : "generic";
    }
}
