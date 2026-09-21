package com.sni.bokaticowork.features.subscription.metrics.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.metrics.dto.SubscriptionMetricsOverviewResponse;
import com.sni.bokaticowork.features.subscription.metrics.service.SubscriptionMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/subscription-metrics")
@RequiredArgsConstructor
public class SubscriptionMetricsController {

    private final SubscriptionMetricsService metricsService;
    private final com.sni.bokaticowork.features.subscription.metrics.service.SubscriptionKpiService kpiService;

    @GetMapping("/overview")
    public ResponseEntity<SubscriptionMetricsOverviewResponse> overview() {
        return ResponseEntity.ok(metricsService.overview());
    }

    /** Attrition, revenu recurrent, valeur vie, occupation · sur une periode. */
    @GetMapping("/kpis")
    public ResponseEntity<com.sni.bokaticowork.features.subscription.metrics.service.SubscriptionKpiService.Kpis> kpis(
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        java.time.LocalDate end = to == null ? java.time.LocalDate.now() : to;
        java.time.LocalDate start = from == null ? end.minusMonths(1).plusDays(1) : from;
        return ResponseEntity.ok(kpiService.compute(start, end));
    }
}
