package com.sni.bokaticowork.core.settings.userSettings.dto.response;

public record NotificationPreferenceResponse(
        String preferenceName,
        String preferenceDescription,
        String eventType,
        String channel,
        Boolean enabled
) {}
