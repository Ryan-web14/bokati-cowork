package com.sni.bokaticowork.features.reporting.service.implementation;

import com.sni.bokaticowork.features.reporting.dto.response.SubscriptionReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.SubscriptionReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.SubscriptionReportMapper;
import com.sni.bokaticowork.features.reporting.repository.SubscriptionReportRepository;
import com.sni.bokaticowork.features.reporting.service.interfaces.SubscriptionReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionReportServiceImpl implements SubscriptionReportService {

    private final SubscriptionReportRepository repository;
    private final SubscriptionReportMapper mapper;

    @Override
    public SubscriptionReportResponse subscriptionReport(int months, int renewalDays) {
        List<MrrDataPoint> mrrTrend = repository.mrrTrend(months).stream()
                .map(mapper::toMrrDataPoint).toList();

        List<Object[]> planRows = repository.activeByPlan();
        BigDecimal totalPlanAmount = planRows.stream()
                .map(r -> decimalAt(r, 2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PlanDistribution> activeByPlan = planRows.stream()
                .map(r -> mapper.toPlanDistribution(r, totalPlanAmount)).toList();

        List<ChurnDataPoint> churnTrend = repository.churnTrend(months).stream()
                .map(mapper::toChurnDataPoint).toList();

        List<UpcomingRenewal> renewals = repository.upcomingRenewals(renewalDays).stream()
                .map(mapper::toUpcomingRenewal).toList();

        List<Object[]> revenueRows = repository.revenueByPlan();
        BigDecimal totalRevenue = revenueRows.stream()
                .map(r -> decimalAt(r, 2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PlanRevenue> revenueByPlan = revenueRows.stream()
                .map(r -> mapper.toPlanRevenue(r, totalRevenue)).toList();

        long totalActive = activeByPlan.stream().mapToLong(PlanDistribution::subscriptionCount).sum();
        BigDecimal currentMrr = mrrTrend.isEmpty() ? BigDecimal.ZERO : mrrTrend.getLast().mrr();

        return new SubscriptionReportResponse(
                Instant.now(), totalActive, currentMrr,
                mrrTrend, activeByPlan, churnTrend, renewals, revenueByPlan
        );
    }

    private BigDecimal decimalAt(Object[] row, int i) {
        Object v = row[i];
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }
}
