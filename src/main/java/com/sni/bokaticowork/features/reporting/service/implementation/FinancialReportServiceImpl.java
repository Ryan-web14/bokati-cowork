package com.sni.bokaticowork.features.reporting.service.implementation;

import com.sni.bokaticowork.features.reporting.dto.response.BillingAgingReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.BillingAgingReportResponse.AgingBucket;
import com.sni.bokaticowork.features.reporting.dto.response.CashFlowReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.CashFlowReportResponse.CashFlowByType;
import com.sni.bokaticowork.features.reporting.dto.response.CashFlowReportResponse.DailyCashFlowEntry;
import com.sni.bokaticowork.features.reporting.dto.response.CashRegisterReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.CashRegisterReportResponse.RegisterSummary;
import com.sni.bokaticowork.features.reporting.dto.response.FinancialDashboardResponse;
import com.sni.bokaticowork.features.reporting.dto.response.FinancialDashboardResponse.PaymentSummary;
import com.sni.bokaticowork.features.reporting.dto.response.FinancialDashboardResponse.RevenueBySource;
import com.sni.bokaticowork.features.reporting.dto.response.PaymentSourceReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.PaymentSourceReportResponse.DailyPaymentEntry;
import com.sni.bokaticowork.features.reporting.dto.response.PaymentSourceReportResponse.MethodBreakdown;
import com.sni.bokaticowork.features.reporting.repository.FinancialReportRepository;
import com.sni.bokaticowork.features.reporting.service.interfaces.FinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private final FinancialReportRepository repository;

    @Override
    public FinancialDashboardResponse dashboard(LocalDate from, LocalDate to) {
        Instant fromInst = toStartInstant(from);
        Instant toInst   = toEndInstant(to);

        Object[] kpi = repository.financialKpis(fromInst, toInst);
        Object[] pmtRow = repository.paymentSummary(fromInst, toInst);

        long invoiceCount   = longAt(kpi, 0);
        BigDecimal invoiced = decimalAt(kpi, 1);
        BigDecimal paid     = decimalAt(kpi, 2);

        BigDecimal collectionRate = invoiced.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : paid.multiply(BigDecimal.valueOf(100)).divide(invoiced, 2, RoundingMode.HALF_UP);

        List<RevenueBySource> revenueBySource = repository.revenueBySource(fromInst, toInst).stream()
                .map(r -> new RevenueBySource(
                        str(r[0]),
                        longAt(r, 1),
                        decimalAt(r, 2),
                        decimalAt(r, 3),
                        decimalAt(r, 4)
                )).toList();

        PaymentSummary payments = new PaymentSummary(
                longAt(pmtRow, 0),
                decimalAt(pmtRow, 1),
                decimalAt(pmtRow, 2),
                decimalAt(pmtRow, 3),
                decimalAt(pmtRow, 4),
                decimalAt(pmtRow, 5),
                decimalAt(pmtRow, 6),
                decimalAt(pmtRow, 7),
                longAt(pmtRow, 8),
                longAt(pmtRow, 9)
        );

        return new FinancialDashboardResponse(
                Instant.now(), from, to,
                invoiceCount,
                invoiced,
                paid,
                decimalAt(kpi, 3),
                longAt(kpi, 4),
                decimalAt(kpi, 5),
                collectionRate,
                decimalAt(kpi, 6),
                decimalAt(kpi, 7),
                decimalAt(kpi, 8),
                longAt(kpi, 9),
                revenueBySource,
                payments
        );
    }

    @Override
    public PaymentSourceReportResponse paymentSources(LocalDate from, LocalDate to) {
        Instant fromInst = toStartInstant(from);
        Instant toInst   = toEndInstant(to);

        List<Object[]> methodRows = repository.paymentByMethod(fromInst, toInst);
        List<Object[]> dailyRows  = repository.paymentDailySeries(fromInst, toInst);

        BigDecimal totalReceived = methodRows.stream()
                .map(r -> decimalAt(r, 2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTransactions = methodRows.stream()
                .mapToLong(r -> longAt(r, 1))
                .sum();

        List<MethodBreakdown> byMethod = methodRows.stream()
                .map(r -> {
                    BigDecimal amount = decimalAt(r, 2);
                    BigDecimal pct = totalReceived.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : amount.multiply(BigDecimal.valueOf(100)).divide(totalReceived, 2, RoundingMode.HALF_UP);
                    return new MethodBreakdown(str(r[0]), longAt(r, 1), amount, pct);
                }).toList();

        List<DailyPaymentEntry> dailySeries = dailyRows.stream()
                .map(r -> new DailyPaymentEntry(
                        toLocalDate(r[0]),
                        decimalAt(r, 1),
                        decimalAt(r, 2),
                        decimalAt(r, 3),
                        decimalAt(r, 4),
                        decimalAt(r, 5),
                        decimalAt(r, 6),
                        decimalAt(r, 7)
                )).toList();

        return new PaymentSourceReportResponse(
                Instant.now(), from, to,
                totalReceived, totalTransactions, byMethod, dailySeries
        );
    }

    @Override
    public BillingAgingReportResponse billingAging() {
        List<Object[]> rows = repository.billingAging();

        Map<String, Object[]> byBucket = new HashMap<>();
        for (Object[] row : rows) {
            byBucket.put(str(row[0]), row);
        }

        BigDecimal total = rows.stream()
                .map(r -> decimalAt(r, 2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalInvoices = rows.stream()
                .mapToLong(r -> longAt(r, 1))
                .sum();

        return new BillingAgingReportResponse(
                Instant.now(),
                total,
                totalInvoices,
                buildBucket("CURRENT",  "Non échu",        byBucket, total),
                buildBucket("1_30",     "1 – 30 jours",    byBucket, total),
                buildBucket("31_60",    "31 – 60 jours",   byBucket, total),
                buildBucket("61_90",    "61 – 90 jours",   byBucket, total),
                buildBucket("90_PLUS",  "+ 90 jours",      byBucket, total)
        );
    }

    @Override
    public CashRegisterReportResponse cashRegisters(LocalDate from, LocalDate to) {
        Instant fromInst = toStartInstant(from);
        Instant toInst   = toEndInstant(to);

        List<Object[]> rows = repository.cashRegisterReport(fromInst, toInst);

        List<RegisterSummary> registers = rows.stream()
                .map(r -> {
                    BigDecimal cashIn  = decimalAt(r, 4);
                    BigDecimal cashOut = decimalAt(r, 5);
                    return new RegisterSummary(
                            str(r[0]),
                            str(r[1]),
                            longAt(r, 2),
                            decimalAt(r, 3),
                            cashIn,
                            cashOut,
                            cashIn.subtract(cashOut),
                            decimalAt(r, 6),
                            longAt(r, 7)
                    );
                }).toList();

        BigDecimal totalIn  = registers.stream().map(RegisterSummary::cashIn).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOut = registers.stream().map(RegisterSummary::cashOut).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CashRegisterReportResponse(
                Instant.now(), from, to,
                totalIn, totalOut, totalIn.subtract(totalOut),
                registers
        );
    }

    @Override
    public CashFlowReportResponse cashFlow(LocalDate from, LocalDate to) {
        Instant fromInst = toStartInstant(from);
        Instant toInst   = toEndInstant(to);

        List<Object[]> typeRows  = repository.cashFlowByType(fromInst, toInst);
        List<Object[]> dailyRows = repository.cashFlowDailySeries(fromInst, toInst);

        List<CashFlowByType> byType = typeRows.stream()
                .map(r -> new CashFlowByType(str(r[0]), str(r[1]), longAt(r, 2), decimalAt(r, 3)))
                .toList();

        BigDecimal totalIn  = byType.stream()
                .filter(t -> "IN".equals(t.direction()))
                .map(CashFlowByType::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOut = byType.stream()
                .filter(t -> "OUT".equals(t.direction()))
                .map(CashFlowByType::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DailyCashFlowEntry> daily = dailyRows.stream()
                .map(r -> {
                    BigDecimal inflows  = decimalAt(r, 1);
                    BigDecimal outflows = decimalAt(r, 2);
                    return new DailyCashFlowEntry(toLocalDate(r[0]), inflows, outflows, inflows.subtract(outflows));
                }).toList();

        return new CashFlowReportResponse(
                Instant.now(), from, to,
                totalIn, totalOut, totalIn.subtract(totalOut),
                byType, daily
        );
    }

    // --- helpers ---

    private AgingBucket buildBucket(String key, String label, Map<String, Object[]> map, BigDecimal total) {
        Object[] row = map.get(key);
        if (row == null) return new AgingBucket(label, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal amount = decimalAt(row, 2);
        BigDecimal pct = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
        return new AgingBucket(label, longAt(row, 1), amount, pct);
    }

    private Instant toStartInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toEndInstant(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
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
        if (v instanceof Date d) return d.toLocalDate();
        if (v instanceof LocalDate ld) return ld;
        return LocalDate.parse(v.toString());
    }
}