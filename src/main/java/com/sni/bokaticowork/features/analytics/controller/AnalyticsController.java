package com.sni.bokaticowork.features.analytics.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.analytics.dto.AnalyticsComparisonResponse;
import com.sni.bokaticowork.features.analytics.dto.AnalyticsLiveResponse;
import com.sni.bokaticowork.features.analytics.dto.AnalyticsOverviewResponse;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1)
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final BookingRepository bookingRepository;

    @GetMapping("/analytics/overview")
    public ResponseEntity<AnalyticsOverviewResponse> overview(@RequestParam(required = false) String fromDate,
                                                              @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(analyticsService.overview(parseDate(fromDate), parseDate(toDate)));
    }

    @GetMapping("/analytics/comparison")
    public ResponseEntity<AnalyticsComparisonResponse> comparison(@RequestParam String currentFrom,
                                                                  @RequestParam String currentTo,
                                                                  @RequestParam String previousFrom,
                                                                  @RequestParam String previousTo) {
        AnalyticsOverviewResponse current = analyticsService.overview(parseDate(currentFrom), parseDate(currentTo));
        AnalyticsOverviewResponse previous = analyticsService.overview(parseDate(previousFrom), parseDate(previousTo));
        return ResponseEntity.ok(new AnalyticsComparisonResponse(
                current,
                previous,
                variation(current.financial().invoicedAmount(), previous.financial().invoicedAmount()),
                variation(current.financial().paidAmount(), previous.financial().paidAmount()),
                variation(BigDecimal.valueOf(current.booking().bookingCount()), BigDecimal.valueOf(previous.booking().bookingCount())),
                variation(BigDecimal.valueOf(current.subscription().activeSubscriptions()), BigDecimal.valueOf(previous.subscription().activeSubscriptions()))
        ));
    }

    @GetMapping("/analytics/live")
    public ResponseEntity<AnalyticsLiveResponse> live() {
        LocalDateTime now = LocalDateTime.now();
        return ResponseEntity.ok(new AnalyticsLiveResponse(
                Instant.now(),
                bookingRepository.countActiveAt(now),
                bookingRepository.countCheckedInNow(),
                bookingRepository.countOccupiedResourcesAt(now)
        ));
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
