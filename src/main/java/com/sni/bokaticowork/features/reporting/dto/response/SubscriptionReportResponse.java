package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SubscriptionReportResponse(
        Instant generatedAt,
        long totalActive,
        BigDecimal currentMrr,
        List<MrrDataPoint> mrrTrend,
        List<PlanDistribution> activeByPlan,
        List<ChurnDataPoint> churnTrend,
        List<UpcomingRenewal> upcomingRenewals,
        List<PlanRevenue> revenueByPlan
) {

    public record MrrDataPoint(
            LocalDate month,
            BigDecimal mrr,
            long activeCount
    ) {}

    public record PlanDistribution(
            String planName,
            long subscriptionCount,
            BigDecimal totalAmount,
            BigDecimal percentage
    ) {}

    public record ChurnDataPoint(
            LocalDate month,
            long cancelledCount,
            long totalCount,
            BigDecimal churnRate
    ) {}

    public record UpcomingRenewal(
            String subscriptionNumber,
            String subscriberCode,
            String subscriberType,
            String planName,
            BigDecimal totalAmount,
            String currency,
            LocalDate currentPeriodEnd,
            long daysUntilRenewal
    ) {}

    public record PlanRevenue(
            String planName,
            long subscriptionCount,
            BigDecimal totalRevenue,
            BigDecimal percentage
    ) {}
}
