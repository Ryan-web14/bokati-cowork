package com.sni.bokaticowork.features.reporting.mapper.decorator;

import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.BookingSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.ContractSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.InvoiceSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.MemberSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.PaymentSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.StockSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.SubscriptionSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.SupportSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.Variation;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.ActivityReportMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Component
public abstract class ActivityReportMapperDecorator implements ActivityReportMapper {

    private static final Set<String> INBOUND_TYPES = Set.of(
            "RECEIPT", "PURCHASE_RECEIPT", "RETURN_IN", "ADJUSTMENT_IN", "TRANSFER_IN", "PRODUCTION_OUTPUT"
    );

    @Override
    public BookingSection toBookingSection(Object[] row, Variation variation) {
        return new BookingSection(
                longAt(row, 0), longAt(row, 1), longAt(row, 2), longAt(row, 3),
                decimalAt(row, 4), longAt(row, 5), variation
        );
    }

    @Override
    public InvoiceSection toInvoiceSection(Object[] row, Variation variation) {
        return new InvoiceSection(
                longAt(row, 0), decimalAt(row, 1), decimalAt(row, 2),
                decimalAt(row, 3), longAt(row, 4), variation
        );
    }

    @Override
    public PaymentSection toPaymentSection(Object[] row, Variation variation) {
        return new PaymentSection(
                longAt(row, 0), decimalAt(row, 1), decimalAt(row, 2),
                decimalAt(row, 3), decimalAt(row, 4), decimalAt(row, 5),
                decimalAt(row, 6), variation
        );
    }

    @Override
    public SubscriptionSection toSubscriptionSection(Object[] row, Variation variation) {
        return new SubscriptionSection(
                longAt(row, 0), longAt(row, 1), longAt(row, 2), longAt(row, 3), variation
        );
    }

    @Override
    public StockSection toStockSection(List<Object[]> rows, Variation variation) {
        long inbound = 0, outbound = 0;
        BigDecimal inVal = BigDecimal.ZERO, outVal = BigDecimal.ZERO;
        for (Object[] row : rows) {
            String type = str(row[0]);
            long count = longAt(row, 1);
            BigDecimal value = decimalAt(row, 2);
            if (INBOUND_TYPES.contains(type)) {
                inbound += count;
                inVal = inVal.add(value);
            } else {
                outbound += count;
                outVal = outVal.add(value);
            }
        }
        return new StockSection(inbound, outbound, inVal, outVal, variation);
    }

    @Override
    public ContractSection toContractSection(Object[] row, Variation variation) {
        return new ContractSection(longAt(row, 0), longAt(row, 1), longAt(row, 2), variation);
    }

    @Override
    public MemberSection toMemberSection(Object[] row, Variation variation) {
        return new MemberSection(longAt(row, 0), longAt(row, 1), variation);
    }

    @Override
    public SupportSection toSupportSection(Object[] row, Variation variation) {
        return new SupportSection(longAt(row, 0), longAt(row, 1), longAt(row, 2), longAt(row, 3), variation);
    }

    @Override
    public Variation computeVariation(BigDecimal current, BigDecimal previous) {
        if (previous == null || current == null) return null;
        BigDecimal change = previous.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : current.subtract(previous)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(previous, 2, RoundingMode.HALF_UP);
        return new Variation(previous, current, change);
    }

    protected long longAt(Object[] row, int i) {
        Object v = row[i];
        return v == null ? 0L : ((Number) v).longValue();
    }

    protected BigDecimal decimalAt(Object[] row, int i) {
        Object v = row[i];
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }

    protected String str(Object v) {
        return v == null ? null : v.toString();
    }
}
