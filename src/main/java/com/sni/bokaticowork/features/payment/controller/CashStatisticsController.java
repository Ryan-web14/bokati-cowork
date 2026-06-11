package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterStatisticsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionStatisticsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashierStatisticsResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.CashStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers/statistics")
@RequiredArgsConstructor
public class CashStatisticsController {

    private final CashStatisticsService cashStatisticsService;

    @GetMapping("/sessions/{sessionNumber}")
    public ResponseEntity<CashSessionStatisticsResponse> sessionStatistics(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(cashStatisticsService.sessionStatistics(sessionNumber));
    }

    @GetMapping("/cashiers/{cashierCode}")
    public ResponseEntity<CashierStatisticsResponse> cashierStatistics(@PathVariable String cashierCode) {
        return ResponseEntity.ok(cashStatisticsService.cashierStatistics(cashierCode));
    }

    @GetMapping("/registers/{registerCode}")
    public ResponseEntity<CashRegisterStatisticsResponse> registerStatistics(
            @PathVariable String registerCode,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate) {
        return ResponseEntity.ok(cashStatisticsService.registerStatistics(registerCode, fromDate, toDate));
    }
}
