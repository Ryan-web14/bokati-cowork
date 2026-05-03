package com.sni.bokaticowork.features.reporting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.reporting.dto.response.OccupancyReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.OccupancyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/reports/occupancy")
public class OccupancyReportController {

    private final OccupancyReportService service;

    /**
     * GET /reports/occupancy?from=2026-04-01&to=2026-04-30
     * Resource occupancy rate: booked minutes vs available (8h/day), per type and per resource.
     */
    @GetMapping
    public ResponseEntity<OccupancyReportResponse> occupancy(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(service.occupancy(parseDate(from), parseDate(to)));
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }
}