package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.request.AdjustmentReasonRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryAdjustmentApprovalRuleRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryPeriodReopenRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryPeriodRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.AdjustmentReasonResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryAdjustmentApprovalRuleResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryPeriodResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockJournalEntryResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockValuationSnapshotResponse;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryPeriodService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockAccountingService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockValuationSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Periodes comptables, photos de valorisation, ecritures de stock et parametrage des ajustements.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/accounting")
public class InventoryAccountingController {

    private final InventoryPeriodService periodService;
    private final StockAccountingService accountingService;
    private final StockValuationSettingsService valuationSettingsService;

    // Periodes

    @PostMapping("/periods")
    @Audited(module = "INVENTORY", action = "CREATE_PERIOD", ressource = "inventory_period")
    @Idempotent(operation = "INVENTORY_PERIOD_CREATE", required = false)
    public ResponseEntity<InventoryPeriodResponse> createPeriod(@Valid @RequestBody InventoryPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodService.create(request));
    }

    @GetMapping("/periods")
    public ResponseEntity<List<InventoryPeriodResponse>> listPeriods() {
        return ResponseEntity.ok(periodService.list());
    }

    @GetMapping("/periods/{periodCode}")
    public ResponseEntity<InventoryPeriodResponse> getPeriod(@PathVariable String periodCode) {
        return ResponseEntity.ok(periodService.get(periodCode));
    }

    /**
     * Clot la periode : fige la valeur du stock et interdit tout mouvement date dans l'intervalle.
     */
    @PatchMapping("/periods/{periodCode}/close")
    @Audited(module = "INVENTORY", action = "CLOSE_PERIOD", ressource = "inventory_period")
    @Idempotent(operation = "INVENTORY_PERIOD_CLOSE", required = false)
    public ResponseEntity<InventoryPeriodResponse> closePeriod(@PathVariable String periodCode,
                                                               @RequestParam(required = false) String closedBy) {
        return ResponseEntity.ok(periodService.close(periodCode, closedBy));
    }

    @PatchMapping("/periods/{periodCode}/reopen")
    @Audited(module = "INVENTORY", action = "REOPEN_PERIOD", ressource = "inventory_period")
    @Idempotent(operation = "INVENTORY_PERIOD_REOPEN", required = false)
    public ResponseEntity<InventoryPeriodResponse> reopenPeriod(@PathVariable String periodCode,
                                                                @Valid @RequestBody InventoryPeriodReopenRequest request) {
        return ResponseEntity.ok(periodService.reopen(periodCode, request));
    }

    // Photos de valorisation

    @PostMapping("/snapshots")
    @Audited(module = "INVENTORY", action = "TAKE_VALUATION_SNAPSHOT", ressource = "stock_valuation_snapshot")
    @Idempotent(operation = "INVENTORY_SNAPSHOT_TAKE", required = false)
    public ResponseEntity<List<StockValuationSnapshotResponse>> takeSnapshot(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(required = false) String periodCode) {
        return ResponseEntity.ok(periodService.takeSnapshot(snapshotDate, periodCode));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<StockValuationSnapshotResponse>> snapshotAt(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
        return ResponseEntity.ok(periodService.snapshotAt(snapshotDate));
    }

    // Ecritures

    @GetMapping("/journal")
    public ResponseEntity<List<StockJournalEntryResponse>> journal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String periodCode) {
        return ResponseEntity.ok(accountingService.export(fromDate, toDate, periodCode));
    }

    @GetMapping("/journal.csv")
    public ResponseEntity<String> journalCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String periodCode) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-journal.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(accountingService.exportCsv(fromDate, toDate, periodCode));
    }

    // Motifs d'ajustement

    @PostMapping("/adjustment-reasons")
    @Audited(module = "INVENTORY", action = "CREATE_ADJUSTMENT_REASON", ressource = "inventory_adjustment_reason")
    @Idempotent(operation = "INVENTORY_ADJUSTMENT_REASON_CREATE", required = false)
    public ResponseEntity<AdjustmentReasonResponse> createReason(@Valid @RequestBody AdjustmentReasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(valuationSettingsService.createReason(request));
    }

    @GetMapping("/adjustment-reasons")
    public ResponseEntity<List<AdjustmentReasonResponse>> listReasons(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(valuationSettingsService.listReasons(active));
    }

    // Seuils d'approbation

    @PostMapping("/adjustment-approval-rules")
    @Audited(module = "INVENTORY", action = "CREATE_ADJUSTMENT_RULE", ressource = "inventory_adjustment_approval_rule")
    @Idempotent(operation = "INVENTORY_ADJUSTMENT_RULE_CREATE", required = false)
    public ResponseEntity<InventoryAdjustmentApprovalRuleResponse> createRule(
            @Valid @RequestBody InventoryAdjustmentApprovalRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(valuationSettingsService.createApprovalRule(request));
    }

    @GetMapping("/adjustment-approval-rules")
    public ResponseEntity<List<InventoryAdjustmentApprovalRuleResponse>> listRules() {
        return ResponseEntity.ok(valuationSettingsService.listApprovalRules());
    }
}
