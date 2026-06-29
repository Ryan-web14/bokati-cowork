package com.sni.bokaticowork.core.event.dto;

import java.time.Instant;

public record InventoryAlertEvent(
        String alertCode,
        String alertType,
        String itemCode,
        String locationCode,
        String assetCode,
        String message,
        Instant createdAt
) {}
