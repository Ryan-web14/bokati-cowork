package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ResourceUtilizationReportResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        int periodDays,
        BigDecimal overallOccupancyRate,
        List<ResourceUtilization> resources,
        List<HourlyHeatmap> hourlyHeatmap,
        List<UnderutilizedResource> underutilized
) {

    public record ResourceUtilization(
            String resourceCode,
            String resourceName,
            String resourceType,
            long totalBookings,
            long morningBookings,
            long afternoonBookings,
            long eveningBookings,
            long bookedMinutes,
            long availableMinutes,
            BigDecimal occupancyRate,
            BigDecimal totalRevenue,
            BigDecimal revenuePerHour
    ) {}

    public record HourlyHeatmap(
            int hour,
            long bookingCount,
            BigDecimal percentage
    ) {}

    public record UnderutilizedResource(
            String resourceCode,
            String resourceName,
            String resourceType,
            long bookedMinutes,
            BigDecimal occupancyRate,
            BigDecimal totalRevenue
    ) {}
}
