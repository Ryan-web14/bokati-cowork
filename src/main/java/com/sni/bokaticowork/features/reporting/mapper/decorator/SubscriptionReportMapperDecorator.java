package com.sni.bokaticowork.features.reporting.mapper.decorator;

import com.sni.bokaticowork.features.reporting.dto.response.SubscriptionReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.SubscriptionReportMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;

@Component
public abstract class SubscriptionReportMapperDecorator implements SubscriptionReportMapper {

    @Override
    public MrrDataPoint toMrrDataPoint(Object[] row) {
        return new MrrDataPoint(
                toLocalDate(row[0]),
                decimalAt(row, 1),
                longAt(row, 2)
        );
    }

    @Override
    public PlanDistribution toPlanDistribution(Object[] row, BigDecimal totalAmount) {
        BigDecimal amount = decimalAt(row, 2);
        BigDecimal pct = totalAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : amount.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP);
        return new PlanDistribution(str(row[0]), longAt(row, 1), amount, pct);
    }

    @Override
    public ChurnDataPoint toChurnDataPoint(Object[] row) {
        long cancelled = longAt(row, 1);
        long total = longAt(row, 2);
        BigDecimal churnRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(cancelled)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new ChurnDataPoint(toLocalDate(row[0]), cancelled, total, churnRate);
    }

    @Override
    public UpcomingRenewal toUpcomingRenewal(Object[] row) {
        return new UpcomingRenewal(
                str(row[0]), str(row[1]), str(row[2]), str(row[3]),
                decimalAt(row, 4), str(row[5]),
                toLocalDate(row[6]), longAt(row, 7)
        );
    }

    @Override
    public PlanRevenue toPlanRevenue(Object[] row, BigDecimal totalRevenue) {
        BigDecimal revenue = decimalAt(row, 2);
        BigDecimal pct = totalRevenue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : revenue.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP);
        return new PlanRevenue(str(row[0]), longAt(row, 1), revenue, pct);
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

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof Date d) return d.toLocalDate();
        if (v instanceof LocalDate ld) return ld;
        return LocalDate.parse(v.toString());
    }
}
