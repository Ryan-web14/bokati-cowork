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

    @GetMapping("/overview")
    public ResponseEntity<SubscriptionMetricsOverviewResponse> overview() {
        return ResponseEntity.ok(metricsService.overview());
    }
}
