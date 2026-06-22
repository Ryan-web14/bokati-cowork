package com.sni.bokaticowork.features.reporting.mapper.decorator;

import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse.HourlyHeatmap;
import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse.ResourceUtilization;
import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse.UnderutilizedResource;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.ResourceUtilizationReportMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public abstract class ResourceUtilizationReportMapperDecorator implements ResourceUtilizationReportMapper {

    private static final long OPERATING_MINUTES_PER_DAY = 8 * 60L;

    @Override
    public ResourceUtilization toResourceUtilization(Object[] row, int periodDays) {
        long bookedMinutes = longAt(row, 7);
        long availableMinutes = (long) periodDays * OPERATING_MINUTES_PER_DAY;
        BigDecimal occupancyRate = occupancyRate(bookedMinutes, availableMinutes);
        BigDecimal revenue = decimalAt(row, 8);
        BigDecimal revenuePerHour = bookedMinutes == 0
                ? BigDecimal.ZERO
                : revenue.multiply(BigDecimal.valueOf(60))
                        .divide(BigDecimal.valueOf(bookedMinutes), 2, RoundingMode.HALF_UP);

        return new ResourceUtilization(
                str(row[0]), str(row[1]), str(row[2]),
                longAt(row, 3), longAt(row, 4), longAt(row, 5), longAt(row, 6),
                bookedMinutes, availableMinutes, occupancyRate,
                revenue, revenuePerHour
        );
    }

    @Override
    public HourlyHeatmap toHourlyHeatmap(Object[] row, long totalBookings) {
        long count = longAt(row, 1);
        BigDecimal pct = totalBookings == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(count)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP);
        return new HourlyHeatmap(intAt(row, 0), count, pct);
    }

    @Override
    public UnderutilizedResource toUnderutilizedResource(ResourceUtilization r) {
        return new UnderutilizedResource(
                r.resourceCode(), r.resourceName(), r.resourceType(),
                r.bookedMinutes(), r.occupancyRate(), r.totalRevenue()
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

    private int intAt(Object[] row, int i) {
        Object v = row[i];
        return v == null ? 0 : ((Number) v).intValue();
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
