package com.sni.bokaticowork.features.analytics.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.analytics.dto.AnalyticsOverviewResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1)
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analytics/overview")
    public ResponseEntity<AnalyticsOverviewResponse> overview(@RequestParam(required = false) String fromDate,
                                                              @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(analyticsService.overview(parseDate(fromDate), parseDate(toDate)));
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
}
