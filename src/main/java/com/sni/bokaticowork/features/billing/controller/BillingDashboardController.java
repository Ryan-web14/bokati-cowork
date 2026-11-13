package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.service.implementation.BillingDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Lot C15 : tableau de bord commercial. Par defaut le mois en cours, en XAF ; une devise a la fois.
 * {@code weeks} : horizon de la prevision d'encaissement (1 a 26 semaines, 8 par defaut).
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing/dashboard")
@RequiredArgsConstructor
public class BillingDashboardController {

    private final BillingDashboardService dashboard;

    @GetMapping
    public ResponseEntity<BillingDashboardService.Dashboard> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "8") int weeks) {
        return ResponseEntity.ok(dashboard.dashboard(from, to, currency, weeks));
    }
}
