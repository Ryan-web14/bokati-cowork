package com.sni.bokaticowork.features.analytics.dto;

import java.math.BigDecimal;

public record TopOwnerResponse(
        String ownerType,
        String ownerCode,
        String ownerName,
        long bookingCount,
        long confirmedCount,
        long completedCount,
        long cancelledCount,
        BigDecimal revenue,
        long bookedMinutes
) {
}
