package com.sni.bokaticowork.features.analytics.dto;

import java.time.Instant;

public record AnalyticsLiveResponse(
        Instant generatedAt,
        long activeBookings,
        long checkedInBookings,
        long occupiedResources
) {
}
