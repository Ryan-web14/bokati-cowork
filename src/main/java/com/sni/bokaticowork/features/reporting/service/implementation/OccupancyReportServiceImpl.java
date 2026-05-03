package com.sni.bokaticowork.features.reporting.service.implementation;

import com.sni.bokaticowork.features.reporting.dto.response.OccupancyReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.OccupancyReportResponse.ResourceOccupancy;
import com.sni.bokaticowork.features.reporting.dto.response.OccupancyReportResponse.ResourceTypeOccupancy;
import com.sni.bokaticowork.features.reporting.repository.OccupancyReportRepository;
import com.sni.bokaticowork.features.reporting.service.interfaces.OccupancyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OccupancyReportServiceImpl implements OccupancyReportService {

    // Standard coworking operating window assumed for occupancy rate denominator
    private static final long OPERATING_MINUTES_PER_DAY = 8 * 60L; // 8h

    private final OccupancyReportRepository repository;

    @Override
    public OccupancyReportResponse occupancy(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from == null ? null : from.atStartOfDay();
        LocalDateTime toDt   = to   == null ? null : to.atTime(LocalTime.MAX);

        Object[] totals   = repository.totals(fromDt, toDt);
        List<Object[]> byTypeRows     = repository.byResourceType(fromDt, toDt);
        List<Object[]> byResourceRows = repository.byResource(fromDt, toDt);

        long totalBookings     = longAt(totals, 0);
        long totalBookedMinutes = longAt(totals, 1);

        int periodDays = (from == null || to == null) ? 30
                : (int) (to.toEpochDay() - from.toEpochDay() + 1);

        long totalResources = byTypeRows.stream().mapToLong(r -> longAt(r, 2)).sum();
        long totalAvailableMinutes = totalResources * periodDays * OPERATING_MINUTES_PER_DAY;

        BigDecimal overallRate = occupancyRate(totalBookedMinutes, totalAvailableMinutes);

        List<ResourceTypeOccupancy> byType = byTypeRows.stream()
                .map(r -> {
                    long rc       = longAt(r, 2);
                    long minutes  = longAt(r, 4);
                    long available = rc * periodDays * OPERATING_MINUTES_PER_DAY;
                    return new ResourceTypeOccupancy(
                            str(r[0]), str(r[1]),
                            rc,
                            longAt(r, 3),
                            minutes,
                            decimalAt(r, 5),
                            occupancyRate(minutes, available)
                    );
                }).toList();

        List<ResourceOccupancy> byResource = byResourceRows.stream()
                .map(r -> {
                    long booked    = longAt(r, 5);
                    long available = periodDays * OPERATING_MINUTES_PER_DAY;
                    return new ResourceOccupancy(
                            str(r[0]), str(r[1]),
                            str(r[2]), str(r[3]),
                            longAt(r, 4),
                            booked,
                            available,
                            occupancyRate(booked, available),
                            decimalAt(r, 6)
                    );
                }).toList();

        return new OccupancyReportResponse(
                java.time.Instant.now(),
                from, to,
                periodDays,
                totalBookings,
                totalBookedMinutes,
                overallRate,
                byType,
                byResource
        );
    }

    private BigDecimal occupancyRate(long bookedMinutes, long availableMinutes) {
        if (availableMinutes == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(bookedMinutes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(availableMinutes), 2, RoundingMode.HALF_UP);
    }

    private long longAt(Object[] row, int i) {
        Object v = row[i];
        return v == null ? 0L : ((Number) v).longValue();
    }

    private BigDecimal decimalAt(Object[] row, int i) {
        Object v = row[i];
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }

    private String str(Object v) {
        return v == null ? null : v.toString();
    }
}