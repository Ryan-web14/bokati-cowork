package com.sni.bokaticowork.features.reporting.service.implementation;

import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.ResourceUtilizationReportMapper;
import com.sni.bokaticowork.features.reporting.repository.ResourceUtilizationReportRepository;
import com.sni.bokaticowork.features.reporting.service.interfaces.ResourceUtilizationReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ResourceUtilizationReportServiceImpl implements ResourceUtilizationReportService {

    private static final long OPERATING_MINUTES_PER_DAY = 8 * 60L;
    private static final BigDecimal UNDERUTILIZATION_THRESHOLD = BigDecimal.valueOf(20);

    private final ResourceUtilizationReportRepository repository;
    private final ResourceUtilizationReportMapper mapper;

    @Override
    public ResourceUtilizationReportResponse resourceUtilization(LocalDate from, LocalDate to) {
        Instant fromInst = toStartInstant(from);
        Instant toInst = toEndInstant(to);

        int periodDays = (from == null || to == null) ? 30
                : (int) (to.toEpochDay() - from.toEpochDay() + 1);

        List<ResourceUtilization> resources = repository.utilizationByResource(fromInst, toInst)
                .stream().map(row -> mapper.toResourceUtilization(row, periodDays)).toList();

        List<Object[]> peakRows = repository.peakHours(fromInst, toInst);
        long totalBookings = peakRows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<HourlyHeatmap> hourly = peakRows.stream()
                .map(row -> mapper.toHourlyHeatmap(row, totalBookings)).toList();

        List<UnderutilizedResource> underutilized = resources.stream()
                .filter(r -> r.occupancyRate().compareTo(UNDERUTILIZATION_THRESHOLD) < 0)
                .map(mapper::toUnderutilizedResource).toList();

        long totalBooked = resources.stream().mapToLong(ResourceUtilization::bookedMinutes).sum();
        long totalAvailable = (long) resources.size() * periodDays * OPERATING_MINUTES_PER_DAY;
        BigDecimal overallRate = totalAvailable == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalBooked).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalAvailable), 2, RoundingMode.HALF_UP);

        return new ResourceUtilizationReportResponse(
                Instant.now(), from, to, periodDays,
                overallRate, resources, hourly, underutilized
        );
    }

    private Instant toStartInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toEndInstant(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
    }
}
