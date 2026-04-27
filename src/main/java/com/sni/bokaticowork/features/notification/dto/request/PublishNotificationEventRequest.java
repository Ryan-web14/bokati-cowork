package com.sni.bokaticowork.features.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record PublishNotificationEventRequest(
        @NotBlank String eventType,
        @NotBlank String aggregateType,
        @NotBlank String aggregateId,
        Map<String, Object> payload
) {
}
