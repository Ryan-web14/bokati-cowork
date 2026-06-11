package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.ReviewCashAnomalyRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashAnomalyFlagResponse;
import com.sni.bokaticowork.features.payment.enums.CashAnomalySeverity;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyStatus;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyType;
import com.sni.bokaticowork.features.payment.service.interfaces.CashAnomalyDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers/anomalies")
@RequiredArgsConstructor
public class CashAnomalyFlagController {

    private final CashAnomalyDetectionService anomalyDetectionService;

    @PostMapping("/sessions/{sessionNumber}/analyze")
    public ResponseEntity<List<CashAnomalyFlagResponse>> analyzeSession(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(anomalyDetectionService.analyzeSession(sessionNumber));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<CashAnomalyFlagResponse>> list(
            @RequestParam(required = false) String registerCode,
            @RequestParam(required = false) String sessionNumber,
            @RequestParam(required = false) CashAnomalySeverity severity,
            @RequestParam(required = false) CashAnomalyStatus status,
            @RequestParam(required = false) CashAnomalyType anomalyType,
            @PageableDefault(size = 20, sort = "detectedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(anomalyDetectionService.list(registerCode, sessionNumber, severity, status, anomalyType, pageable));
    }

    @PatchMapping("/{flagNumber}/review")
    public ResponseEntity<CashAnomalyFlagResponse> review(@PathVariable String flagNumber,
                                                           @Valid @RequestBody ReviewCashAnomalyRequest request) {
        return ResponseEntity.ok(anomalyDetectionService.review(flagNumber, request));
    }
}
