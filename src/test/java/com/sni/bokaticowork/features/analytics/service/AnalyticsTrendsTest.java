package com.sni.bokaticowork.features.analytics.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.analytics.dto.OccupancyTrendResponse;
import com.sni.bokaticowork.features.analytics.dto.RevenueTrendResponse;
import com.sni.bokaticowork.features.analytics.repository.AnalyticsRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tendances du chiffre d'affaires et de l'occupation : periodes completes, valeurs et taux. */
class AnalyticsTrendsTest {

    private final AnalyticsRepository repository = mock(AnalyticsRepository.class);
    private final AnalyticsServiceImpl service = new AnalyticsServiceImpl(repository);

    @Test
    void everyDayOfTheRangeIsPresentEvenWithoutActivity() {
        when(repository.invoicedByPeriod(eq("day"), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{Date.valueOf("2026-11-15"), 2L, new BigDecimal("90000")}));
        when(repository.collectedByPeriod(eq("day"), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{Date.valueOf("2026-11-16"), 1L, new BigDecimal("45000")}));

        List<RevenueTrendResponse> trend = service.revenueTrend(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 16), "DAY");

        assertThat(trend).extracting(RevenueTrendResponse::date)
                .containsExactly(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16));
        assertThat(trend.get(0).invoicedAmount()).isZero();
        assertThat(trend.get(1).invoiceCount()).isEqualTo(2);
        assertThat(trend.get(1).invoicedAmount()).isEqualByComparingTo("90000");
        assertThat(trend.get(2).collectedAmount()).isEqualByComparingTo("45000");
    }

    @Test
    void weeksStartOnMondayAndAreCutToTheRange() {
        List<TrendPeriods.Period> periods = TrendPeriods.periods(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 11, 16), "week");

        assertThat(periods.getFirst()).isEqualTo(new TrendPeriods.Period(LocalDate.of(2026, 10, 12), 2));
        assertThat(periods.get(1)).isEqualTo(new TrendPeriods.Period(LocalDate.of(2026, 10, 19), 7));
        assertThat(periods.getLast()).isEqualTo(new TrendPeriods.Period(LocalDate.of(2026, 11, 16), 1));
        assertThat(periods.stream().mapToInt(TrendPeriods.Period::days).sum()).isEqualTo(31);
    }

    @Test
    void occupancyUsesEightHoursPerResourceAndDay() {
        when(repository.occupancyResourceCount()).thenReturn(5L);
        when(repository.occupancyByPeriod(eq("month"), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{Date.valueOf("2026-11-01"), 5L, 840L}));

        List<OccupancyTrendResponse> trend = service.occupancyTrend(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "MONTH");

        assertThat(trend).hasSize(1);
        OccupancyTrendResponse november = trend.getFirst();
        assertThat(november.days()).isEqualTo(30);
        assertThat(november.availableMinutes()).isEqualTo(5L * 30 * 480);
        assertThat(november.occupancyRate()).isEqualByComparingTo("1.17");
    }

    @Test
    void theLastThirtyDaysByDefault() {
        List<OccupancyTrendResponse> trend = service.occupancyTrend(null, null, null);
        assertThat(trend).hasSize(30);
        assertThat(trend.getLast().date()).isEqualTo(LocalDate.now());
    }

    @Test
    void badIntervalsAndReversedRangesAreRefused() {
        assertThatThrownBy(() -> service.revenueTrend(null, null, "YEAR")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.revenueTrend(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 1), "DAY"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.revenueTrend(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1), "DAY"))
                .isInstanceOf(BadRequestException.class);
    }
}
