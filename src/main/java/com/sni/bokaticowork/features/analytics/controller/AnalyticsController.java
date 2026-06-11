package com.sni.bokaticowork.features.analytics.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.analytics.dto.AnalyticsComparisonResponse;
import com.sni.bokaticowork.features.analytics.dto.AnalyticsLiveResponse;
import com.sni.bokaticowork.features.analytics.dto.AnalyticsOverviewResponse;
import com.sni.bokaticowork.features.analytics.dto.BookingTrendResponse;
import com.sni.bokaticowork.features.analytics.dto.TopOwnerResponse;
import com.sni.bokaticowork.features.analytics.dto.TopResourceResponse;
import com.sni.bokaticowork.features.analytics.repository.AnalyticsRepository;
import com.sni.bokaticowork.features.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1)
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsRepository analyticsRepository;

    @GetMapping("/analytics/overview")
    public ResponseEntity<AnalyticsOverviewResponse> overview(@RequestParam(required = false) String fromDate,
                                                              @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(analyticsService.overview(parseDate(fromDate), parseDate(toDate)));
    }

    @GetMapping("/analytics/comparison")
    public ResponseEntity<AnalyticsComparisonResponse> comparison(
            @RequestParam String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "false") boolean compareWithPrevious) {
        LocalDate currentFrom = LocalDate.parse(fromDate.trim());
        LocalDate currentTo = toDate != null && !toDate.isBlank() ? LocalDate.parse(toDate.trim()) : LocalDate.now();

        AnalyticsOverviewResponse current = analyticsService.overview(currentFrom, currentTo);
        AnalyticsOverviewResponse previous = null;
        if (compareWithPrevious) {
            long periodDays = currentFrom.until(currentTo, java.time.temporal.ChronoUnit.DAYS);
            LocalDate previousTo = currentFrom.minusDays(1);
            LocalDate previousFrom = previousTo.minusDays(periodDays);
            previous = analyticsService.overview(previousFrom, previousTo);
        }

        AnalyticsOverviewResponse safePrevious = previous != null ? previous : emptyOverview();
        return ResponseEntity.ok(new AnalyticsComparisonResponse(
                current,
                safePrevious,
                variation(current.financial().invoicedAmount(), safePrevious.financial().invoicedAmount()),
                variation(current.financial().paidAmount(), safePrevious.financial().paidAmount()),
                variation(BigDecimal.valueOf(current.booking().bookingCount()), BigDecimal.valueOf(safePrevious.booking().bookingCount())),
                variation(BigDecimal.valueOf(current.subscription().activeSubscriptions()), BigDecimal.valueOf(safePrevious.subscription().activeSubscriptions()))
        ));
    }

    @GetMapping("/analytics/live")
    public ResponseEntity<AnalyticsLiveResponse> live() {
        Object[] r = analyticsRepository.live();
        long occupied = asLong(r[2]);
        long bookable = asLong(r[3]);
        BigDecimal rate = bookable == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(occupied * 100.0 / bookable)
                        .setScale(1, RoundingMode.HALF_UP);
        return ResponseEntity.ok(new AnalyticsLiveResponse(
                Instant.now(),
                asLong(r[0]),                       // activeBookings
                asLong(r[1]),                       // checkedInBookings
                occupied,                           // occupiedResources
                bookable,                           // totalBookableResources
                rate,                               // occupancyRate %
                Math.max(0, bookable - occupied),   // availableResources
                asLong(r[4]),                       // pendingApproval
                asLong(r[5]),                       // upcomingNextHour
                asLong(r[6]),                       // activeHolds
                asLong(r[7]),                       // bookingsToday
                asDecimal(r[8]),                    // revenueToday
                asDecimal(r[9]),                    // avgBookingAmount
                asLong(r[10]),                      // bookedMinutesToday
                asLong(r[11]),                      // cancelledToday
                asLong(r[12]),                      // noShowToday
                asLong(r[13]),                      // checkInsToday
                asLong(r[14])                       // newMembersToday
        ));
    }

    private long asLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private BigDecimal asDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }

    @GetMapping("/reports/financial/summary")
    public ResponseEntity<AnalyticsOverviewResponse.FinancialMetrics> financial(@RequestParam(required = false) String fromDate,
                                                                               @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(analyticsService.financial(parseStart(fromDate), parseEnd(toDate)));
    }

    @GetMapping("/reports/attendance/summary")
    public ResponseEntity<AnalyticsOverviewResponse.BookingMetrics> attendance(@RequestParam(required = false) String fromDate,
                                                                               @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(analyticsService.booking(parseStart(fromDate), parseEnd(toDate)));
    }

    @GetMapping("/analytics/trends/bookings")
    public ResponseEntity<List<BookingTrendResponse>> bookingTrend(@RequestParam(required = false) String fromDate,
                                                                   @RequestParam(required = false) String toDate,
                                                                   @RequestParam(defaultValue = "day") String groupBy) {
        return ResponseEntity.ok(analyticsService.bookingTrend(parseStart(fromDate), parseEnd(toDate), groupBy));
    }

    @GetMapping("/analytics/top-owners")
    public ResponseEntity<List<TopOwnerResponse>> topOwners(@RequestParam(required = false) String fromDate,
                                                            @RequestParam(required = false) String toDate,
                                                            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.topOwners(parseStart(fromDate), parseEnd(toDate), limit));
    }

    @GetMapping("/analytics/top-resources")
    public ResponseEntity<List<TopResourceResponse>> topResources(@RequestParam(required = false) String fromDate,
                                                                  @RequestParam(required = false) String toDate,
                                                                  @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.topResources(parseStart(fromDate), parseEnd(toDate), limit));
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }

    private Instant parseStart(String value) {
        LocalDate date = parseDate(value);
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant parseEnd(String value) {
        LocalDate date = parseDate(value);
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
    }

    private AnalyticsOverviewResponse emptyOverview() {
        BigDecimal zero = BigDecimal.ZERO;
        return new AnalyticsOverviewResponse(
                Instant.now(), null, null,
                new AnalyticsOverviewResponse.FinancialMetrics(0, zero, zero, zero, 0, 0, zero, zero, zero),
                new AnalyticsOverviewResponse.PaymentMetrics(0, zero, zero, zero, zero, zero, zero, 0, 0),
                new AnalyticsOverviewResponse.WalletMetrics(0, zero, zero, zero, zero),
                new AnalyticsOverviewResponse.BookingMetrics(0, 0, 0, 0, 0, zero, 0, 0),
                new AnalyticsOverviewResponse.SubscriptionMetrics(0, 0, 0, 0, zero, 0),
                new AnalyticsOverviewResponse.CustomerMetrics(0, 0, 0, 0),
                new AnalyticsOverviewResponse.InventoryMetrics(0, zero, 0, 0)
        );
    }

    private AnalyticsComparisonResponse.Variation variation(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal safePrevious = previous == null ? BigDecimal.ZERO : previous;
        BigDecimal percent = safePrevious.signum() == 0
                ? BigDecimal.ZERO
                : safeCurrent.subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 2, RoundingMode.HALF_UP);
        return new AnalyticsComparisonResponse.Variation(safeCurrent, safePrevious, percent);
    }
}
