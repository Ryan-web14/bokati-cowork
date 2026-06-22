package com.sni.bokaticowork.features.reporting.mapper.decorator;

import com.sni.bokaticowork.features.reporting.dto.response.StockValuationReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.StockValuationReportMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public abstract class StockValuationReportMapperDecorator implements StockValuationReportMapper {

    @Override
    public CategoryValuation toCategoryValuation(Object[] row, BigDecimal totalValue) {
        BigDecimal value = decimalAt(row, 4);
        BigDecimal pct = totalValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : value.multiply(BigDecimal.valueOf(100)).divide(totalValue, 2, RoundingMode.HALF_UP);
        return new CategoryValuation(
                str(row[0]), str(row[1]),
                longAt(row, 2), decimalAt(row, 3), value, pct
        );
    }

    @Override
    public LocationValuation toLocationValuation(Object[] row, BigDecimal totalValue) {
        BigDecimal value = decimalAt(row, 5);
        BigDecimal pct = totalValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : value.multiply(BigDecimal.valueOf(100)).divide(totalValue, 2, RoundingMode.HALF_UP);
        return new LocationValuation(
                str(row[0]), str(row[1]), str(row[2]),
                longAt(row, 3), decimalAt(row, 4), value, pct
        );
    }

    @Override
    public LowStockItem toLowStockItem(Object[] row) {
        return new LowStockItem(
                str(row[0]), str(row[1]), str(row[2]),
                decimalAt(row, 3), decimalAt(row, 4), decimalAt(row, 5)
        );
    }

    @Override
    public DeadStockItem toDeadStockItem(Object[] row) {
        return new DeadStockItem(
                str(row[0]), str(row[1]), str(row[2]), str(row[3]),
                decimalAt(row, 4), decimalAt(row, 5),
                toLocalDate(row[6]), longAt(row, 7)
        );
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
        if (v instanceof Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (v instanceof OffsetDateTime odt) return odt.toLocalDate();
        if (v instanceof LocalDate ld) return ld;
        return LocalDate.parse(v.toString().substring(0, 10));
    }
}
