package com.sni.bokaticowork.features.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record TestNotificationRequest(
        @Email @NotBlank String to,
        String recipientName,
        String subject,
        String message,
        Map<String, Object> payload
) {
}
