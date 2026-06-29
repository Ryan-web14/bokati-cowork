package com.sni.bokaticowork.core.event.dto;

import java.time.Instant;

public record AdminAlertEvent(
        String alertType,
        String module,
        String title,
        String message,
        String severity,
        String payloadJson,
        Instant createdAt
) {}
