package com.sni.bokaticowork.features.reporting.mapper.decorator;

import com.sni.bokaticowork.features.reporting.dto.response.DebtRecoveryReportResponse.AgingBucket;
import com.sni.bokaticowork.features.reporting.dto.response.DebtRecoveryReportResponse.UnpaidInvoice;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.DebtRecoveryReportMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;

@Component
public abstract class DebtRecoveryReportMapperDecorator implements DebtRecoveryReportMapper {

    @Override
    public UnpaidInvoice toUnpaidInvoice(Object[] row) {
        return new UnpaidInvoice(
                str(row[0]),  // document_number
                str(row[1]),  // customer_name
                str(row[2]),  // customer_code
                str(row[3]),  // customer_type
                str(row[4]),  // customer_email
                str(row[5]),  // customer_phone
                toLocalDate(row[6]),  // issue_date
                toLocalDate(row[7]),  // due_date
                decimalAt(row, 8),    // total_amount
                decimalAt(row, 9),    // paid_amount
                decimalAt(row, 10),   // balance_due
                longAt(row, 11),      // aging_days
                str(row[12])          // aging_bucket
        );
    }

    @Override
    public AgingBucket toAgingBucket(String key, String label, Object[] row, BigDecimal total) {
        if (row == null) return new AgingBucket(label, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal amount = decimalAt(row, 2);
        BigDecimal avg = decimalAt(row, 3);
        BigDecimal pct = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
        return new AgingBucket(label, longAt(row, 1), amount, avg, pct);
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
