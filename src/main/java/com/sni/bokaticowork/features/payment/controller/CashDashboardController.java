package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.response.CashPositionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashReportResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.CashDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Pilotage des caisses : position en temps reel et etats (lot C6 du plan finance). */
@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers")
@RequiredArgsConstructor
public class CashDashboardController {

    private final CashDashboardService service;

    @GetMapping("/position")
    public ResponseEntity<CashPositionResponse> position(@RequestParam(required = false) String locationCode) {
        return ResponseEntity.ok(service.position(locationCode));
    }

    /** Un etat, en JSON ou en CSV ({@code format=csv}). */
    @GetMapping("/reports/{kind}")
    public ResponseEntity<?> report(@PathVariable String kind,
                                    @RequestParam(required = false) String registerCode,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(required = false) String format) {
        CashReportResponse report = service.report(kind, registerCode, from, to);
        if ("csv".equalsIgnoreCase(format)) {
            byte[] body = ("﻿" + service.toCsv(report)).getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"caisse-" + report.kind() + ".csv\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(body);
        }
        return ResponseEntity.ok(report);
    }
}
