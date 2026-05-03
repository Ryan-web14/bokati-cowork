package com.sni.bokaticowork.features.reporting.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OccupancyReportResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        int periodDays,
        long totalBookings,
        long totalBookedMinutes,
        BigDecimal overallOccupancyRate,
        List<ResourceTypeOccupancy> byResourceType,
        List<ResourceOccupancy> byResource
) {

    public record ResourceTypeOccupancy(
            String typeCode,
            String typeName,
            long resourceCount,
            long totalBookings,
            long totalBookedMinutes,
            BigDecimal totalRevenue,
            BigDecimal occupancyRate
    ) {}

    public record ResourceOccupancy(
            String resourceCode,
            String resourceName,
            String typeCode,
            String typeName,
            long bookingCount,
            long bookedMinutes,
            long availableMinutes,
            BigDecimal occupancyRate,
            BigDecimal revenue
    ) {}
}