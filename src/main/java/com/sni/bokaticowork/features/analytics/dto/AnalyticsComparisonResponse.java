package com.sni.bokaticowork.features.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsComparisonResponse(
        AnalyticsOverviewResponse current,
        AnalyticsOverviewResponse previous,
        Variation revenue,
        Variation paid,
        Variation bookings,
        Variation activeSubscriptions
) {
    public record Variation(BigDecimal current, BigDecimal previous, BigDecimal percentChange) {
    }
}
