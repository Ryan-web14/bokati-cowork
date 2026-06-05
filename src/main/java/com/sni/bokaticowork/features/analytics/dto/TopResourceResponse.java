package com.sni.bokaticowork.features.analytics.dto;

import java.math.BigDecimal;

public record TopResourceResponse(
        String resourceCode,
        String resourceName,
        String resourceTypeCode,
        String resourceTypeName,
        String resourceGroupCode,
        String resourceGroupName,
        long bookingCount,
        long confirmedCount,
        long completedCount,
        long cancelledCount,
        BigDecimal revenue,
        long bookedMinutes
) {
}
