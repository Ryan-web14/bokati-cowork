package com.sni.bokaticowork.core.settings.userSettings.dto.request;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;

public record UpdateNotificationPreferenceRequest(
        EventType eventType,
        NotificationChannel channel,
        Boolean enabled
) {}
