package com.sni.bokaticowork.features.notification.dto.request;

import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationTemplateRequest(
        @NotBlank String templateCode,
        @NotNull NotificationChannel channel,
        String subject,
        String templateName,
        String bodyTemplate,
        Boolean active,
        Boolean adminOnly
) {
}
