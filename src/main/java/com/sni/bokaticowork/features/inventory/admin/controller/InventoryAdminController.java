package com.sni.bokaticowork.features.inventory.admin.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryDashboardResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryLabelBatchRequest;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryLabelResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryMovementReportResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryAnomalyReportResponse;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryOverrideReportResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReconciliationReportResponse;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockReconciliationService;
import com.sni.bokaticowork.features.inventory.admin.service.InventoryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/admin")
public class InventoryAdminController {

    private final InventoryAdminService service;
    private final StockReconciliationService reconciliationService;

    @GetMapping("/dashboard")
    public ResponseEntity<InventoryDashboardResponse> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @GetMapping("/labels/{type}/{code}")
    public ResponseEntity<InventoryLabelResponse> label(@PathVariable String type, @PathVariable String code) {
        return ResponseEntity.ok(service.label(type, code));
    }

    @PostMapping("/labels")
    public ResponseEntity<List<InventoryLabelResponse>> labels(@RequestBody InventoryLabelBatchRequest request) {
        return ResponseEntity.ok(service.labels(request));
    }

    @GetMapping("/reports/movements")
    public ResponseEntity<InventoryMovementReportResponse> movementReport(@RequestParam(required = false) String itemCode,
                                                                          @RequestParam(required = false) String locationCode,
                                                                          @RequestParam(required = false) String fromDate,
                                                                          @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(service.movementReport(itemCode, locationCode, parseStart(fromDate), parseEnd(toDate)));
    }

    @GetMapping("/reports/movements.csv")
    public ResponseEntity<String> movementReportCsv(@RequestParam(required = false) String itemCode,
                                                    @RequestParam(required = false) String locationCode,
                                                    @RequestParam(required = false) String fromDate,
                                                    @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-movements.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(service.movementReportCsv(itemCode, locationCode, parseStart(fromDate), parseEnd(toDate)));
    }

    @GetMapping("/reports/movements.pdf")
    public ResponseEntity<byte[]> movementReportPdf(@RequestParam(required = false) String itemCode,
                                                    @RequestParam(required = false) String locationCode,
                                                    @RequestParam(required = false) String fromDate,
                                                    @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-movements.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.movementReportPdf(itemCode, locationCode, parseStart(fromDate), parseEnd(toDate)));
    }

    @GetMapping("/reports/anomalies")
    public ResponseEntity<InventoryAnomalyReportResponse> anomalyReport() {
        return ResponseEntity.ok(service.anomalyReport());
    }

    @GetMapping("/reports/overrides")
    public ResponseEntity<InventoryOverrideReportResponse> overrideReport(@RequestParam(required = false) String referenceType,
                                                                          @RequestParam(required = false) String fromDate,
                                                                          @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(service.overrideReport(referenceType, parseStart(fromDate), parseEnd(toDate)));
    }

    @GetMapping("/reports/overrides.csv")
    public ResponseEntity<String> overrideReportCsv(@RequestParam(required = false) String referenceType,
                                                    @RequestParam(required = false) String fromDate,
                                                    @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-overrides.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(service.overrideReportCsv(referenceType, parseStart(fromDate), parseEnd(toDate)));
    }

    /**
     * Compare les niveaux de stock a la somme des mouvements. Aucun niveau n'est modifie :
     * une divergence est un defaut a analyser, pas une valeur a ecraser.
     */
    @GetMapping("/reconciliation")
    public ResponseEntity<StockReconciliationReportResponse> reconciliation(@RequestParam(required = false) String itemCode,
                                                                            @RequestParam(required = false) String locationCode) {
        return ResponseEntity.ok(reconciliationService.reconcile(itemCode, locationCode));
    }

    @GetMapping("/reconciliation.csv")
    public ResponseEntity<String> reconciliationCsv(@RequestParam(required = false) String itemCode,
                                                    @RequestParam(required = false) String locationCode) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-reconciliation.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reconciliationService.reconcileCsv(itemCode, locationCode));
    }

    private Instant parseStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        return Instant.parse(trimmed);
    }

    private Instant parseEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
        }
        return Instant.parse(trimmed);
    }
}
