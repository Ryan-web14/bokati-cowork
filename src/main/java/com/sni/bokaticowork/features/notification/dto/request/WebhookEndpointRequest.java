package com.sni.bokaticowork.features.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record WebhookEndpointRequest(
        @NotBlank String name,
        @NotBlank String url,
        String secret,
        Set<String> eventTypes,
        Boolean active
) {
}
