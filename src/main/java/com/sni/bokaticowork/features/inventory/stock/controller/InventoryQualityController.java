package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.request.NonConformanceRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.ProductRecallRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.QualityControlPlanRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.QualityInspectionRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.NonConformanceResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.ProductRecallResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.QualityControlPlanResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.QualityInspectionResponse;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.ProductRecallService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.QualityControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controle qualite, non-conformites et rappels produit.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/quality")
public class InventoryQualityController {

    private final QualityControlService qualityService;
    private final ProductRecallService recallService;

    // Plans de controle

    @PostMapping("/plans")
    @Audited(module = "INVENTORY", action = "CREATE_QUALITY_PLAN", ressource = "quality_control_plan")
    @Idempotent(operation = "INVENTORY_QUALITY_PLAN_CREATE", required = false)
    public ResponseEntity<QualityControlPlanResponse> createPlan(@Valid @RequestBody QualityControlPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qualityService.createPlan(request));
    }

    @GetMapping("/plans")
    public ResponseEntity<List<QualityControlPlanResponse>> listPlans() {
        return ResponseEntity.ok(qualityService.listPlans());
    }

    @GetMapping("/plans/{planCode}")
    public ResponseEntity<QualityControlPlanResponse> getPlan(@PathVariable String planCode) {
        return ResponseEntity.ok(qualityService.getPlan(planCode));
    }

    // Inspections

    /**
     * Controle un lot : evalue les criteres, en deduit une decision, et l applique au lot.
     */
    @PostMapping("/lots/{lotId}/inspections")
    @Audited(module = "INVENTORY", action = "INSPECT_LOT", ressource = "quality_inspection")
    @Idempotent(operation = "INVENTORY_QUALITY_INSPECT", required = false)
    public ResponseEntity<QualityInspectionResponse> inspect(@PathVariable Long lotId,
                                                             @Valid @RequestBody QualityInspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qualityService.inspect(lotId, request));
    }

    @GetMapping("/lots/{lotId}/inspections")
    public ResponseEntity<List<QualityInspectionResponse>> inspections(@PathVariable Long lotId) {
        return ResponseEntity.ok(qualityService.inspectionsForLot(lotId));
    }

    // Non-conformites

    @PostMapping("/non-conformances")
    @Audited(module = "INVENTORY", action = "OPEN_NON_CONFORMANCE", ressource = "inventory_non_conformance")
    @Idempotent(operation = "INVENTORY_NON_CONFORMANCE_OPEN", required = false)
    public ResponseEntity<NonConformanceResponse> openNonConformance(@Valid @RequestBody NonConformanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qualityService.openNonConformance(request));
    }

    @PatchMapping("/non-conformances/{code}/close")
    @Audited(module = "INVENTORY", action = "CLOSE_NON_CONFORMANCE", ressource = "inventory_non_conformance")
    @Idempotent(operation = "INVENTORY_NON_CONFORMANCE_CLOSE", required = false)
    public ResponseEntity<NonConformanceResponse> closeNonConformance(
            @PathVariable String code,
            @RequestParam(required = false) String closedBy,
            @RequestParam(required = false) String correctiveAction) {
        return ResponseEntity.ok(qualityService.closeNonConformance(code, closedBy, correctiveAction));
    }

    @GetMapping("/non-conformances")
    public ResponseEntity<Page<NonConformanceResponse>> searchNonConformances(
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) Boolean openOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(qualityService.searchNonConformances(itemCode, openOnly, pageable));
    }

    // Rappels produit

    @PostMapping("/recalls")
    @Audited(module = "INVENTORY", action = "CREATE_RECALL", ressource = "inventory_product_recall")
    @Idempotent(operation = "INVENTORY_RECALL_CREATE", required = false)
    public ResponseEntity<ProductRecallResponse> createRecall(@Valid @RequestBody ProductRecallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recallService.create(request));
    }

    /**
     * Lance le rappel : gele les lots concernes puis liste les detenteurs.
     */
    @PatchMapping("/recalls/{recallCode}/launch")
    @Audited(module = "INVENTORY", action = "LAUNCH_RECALL", ressource = "inventory_product_recall")
    @Idempotent(operation = "INVENTORY_RECALL_LAUNCH", required = false)
    public ResponseEntity<ProductRecallResponse> launchRecall(@PathVariable String recallCode,
                                                              @RequestParam(required = false) String launchedBy) {
        return ResponseEntity.ok(recallService.launch(recallCode, launchedBy));
    }

    @PatchMapping("/recalls/{recallCode}/recovery")
    @Audited(module = "INVENTORY", action = "RECORD_RECALL_RECOVERY", ressource = "inventory_product_recall")
    @Idempotent(operation = "INVENTORY_RECALL_RECOVERY", required = false)
    public ResponseEntity<ProductRecallResponse> recordRecovery(@PathVariable String recallCode,
                                                                @RequestParam BigDecimal quantity) {
        return ResponseEntity.ok(recallService.recordRecovery(recallCode, quantity));
    }

    @PatchMapping("/recalls/{recallCode}/close")
    @Audited(module = "INVENTORY", action = "CLOSE_RECALL", ressource = "inventory_product_recall")
    @Idempotent(operation = "INVENTORY_RECALL_CLOSE", required = false)
    public ResponseEntity<ProductRecallResponse> closeRecall(@PathVariable String recallCode,
                                                             @RequestParam(required = false) String closedBy) {
        return ResponseEntity.ok(recallService.close(recallCode, closedBy));
    }

    @PatchMapping("/recalls/{recallCode}/cancel")
    @Audited(module = "INVENTORY", action = "CANCEL_RECALL", ressource = "inventory_product_recall")
    @Idempotent(operation = "INVENTORY_RECALL_CANCEL", required = false)
    public ResponseEntity<ProductRecallResponse> cancelRecall(@PathVariable String recallCode,
                                                              @RequestParam(required = false) String cancelledBy) {
        return ResponseEntity.ok(recallService.cancel(recallCode, cancelledBy));
    }

    @GetMapping("/recalls")
    public ResponseEntity<List<ProductRecallResponse>> listRecalls() {
        return ResponseEntity.ok(recallService.list());
    }

    @GetMapping("/recalls/{recallCode}")
    public ResponseEntity<ProductRecallResponse> getRecall(@PathVariable String recallCode) {
        return ResponseEntity.ok(recallService.get(recallCode));
    }
}
