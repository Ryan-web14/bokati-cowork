package com.sni.bokaticowork.features.inventory.admin.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryDashboardResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryLabelResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryMovementReportResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryAnomalyReportResponse;
import com.sni.bokaticowork.features.inventory.admin.service.InventoryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/admin")
public class InventoryAdminController {

    private final InventoryAdminService service;

    @GetMapping("/dashboard")
    public ResponseEntity<InventoryDashboardResponse> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @GetMapping("/labels/{type}/{code}")
    public ResponseEntity<InventoryLabelResponse> label(@PathVariable String type, @PathVariable String code) {
        return ResponseEntity.ok(service.label(type, code));
    }

    @GetMapping("/reports/movements")
    public ResponseEntity<InventoryMovementReportResponse> movementReport(@RequestParam(required = false) String itemCode,
                                                                          @RequestParam(required = false) String locationCode,
                                                                          @RequestParam(required = false) Instant fromDate,
                                                                          @RequestParam(required = false) Instant toDate) {
        return ResponseEntity.ok(service.movementReport(itemCode, locationCode, fromDate, toDate));
    }

    @GetMapping(value = "/reports/movements.csv", produces = "text/csv")
    public ResponseEntity<String> movementReportCsv(@RequestParam(required = false) String itemCode,
                                                    @RequestParam(required = false) String locationCode,
                                                    @RequestParam(required = false) Instant fromDate,
                                                    @RequestParam(required = false) Instant toDate) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-movements.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(service.movementReportCsv(itemCode, locationCode, fromDate, toDate));
    }

    @GetMapping(value = "/reports/movements.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> movementReportPdf(@RequestParam(required = false) String itemCode,
                                                    @RequestParam(required = false) String locationCode,
                                                    @RequestParam(required = false) Instant fromDate,
                                                    @RequestParam(required = false) Instant toDate) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-movements.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.movementReportPdf(itemCode, locationCode, fromDate, toDate));
    }

    @GetMapping("/reports/anomalies")
    public ResponseEntity<InventoryAnomalyReportResponse> anomalyReport() {
        return ResponseEntity.ok(service.anomalyReport());
    }
}
