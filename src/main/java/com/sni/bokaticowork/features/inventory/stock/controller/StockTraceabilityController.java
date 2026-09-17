package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotBlockRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotQuarantineRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.LotReleaseRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.LotTraceabilityResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockQuarantineService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockTraceabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tracabilite des lots et immobilisation du stock.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory")
public class StockTraceabilityController {

    private final StockTraceabilityService traceabilityService;
    private final StockQuarantineService quarantineService;

    /**
     * Remonte l amont et redescend l aval d un numero de lot, en un seul appel.
     */
    @GetMapping("/traceability/{lotNumber}")
    public ResponseEntity<LotTraceabilityResponse> trace(@PathVariable String lotNumber) {
        return ResponseEntity.ok(traceabilityService.trace(lotNumber));
    }

    @PatchMapping("/stock/lots/{lotId}/quarantine")
    @Audited(module = "INVENTORY", action = "QUARANTINE_LOT", ressource = "stock_lot")
    @Idempotent(operation = "INVENTORY_LOT_QUARANTINE", required = false)
    public ResponseEntity<StockLotResponse> quarantine(@PathVariable Long lotId,
                                                       @Valid @RequestBody LotQuarantineRequest request) {
        return ResponseEntity.ok(quarantineService.quarantine(lotId, request));
    }

    /**
     * Leve la quarantaine. Exige un approbateur different du demandeur.
     */
    @PatchMapping("/stock/lots/{lotId}/release")
    @Audited(module = "INVENTORY", action = "RELEASE_LOT", ressource = "stock_lot")
    @Idempotent(operation = "INVENTORY_LOT_RELEASE", required = false)
    public ResponseEntity<StockLotResponse> release(@PathVariable Long lotId,
                                                    @Valid @RequestBody LotReleaseRequest request) {
        return ResponseEntity.ok(quarantineService.release(lotId, request));
    }

    @PatchMapping("/stock/lots/{lotId}/block")
    @Audited(module = "INVENTORY", action = "BLOCK_LOT", ressource = "stock_lot")
    @Idempotent(operation = "INVENTORY_LOT_BLOCK", required = false)
    public ResponseEntity<StockLotResponse> block(@PathVariable Long lotId,
                                                  @Valid @RequestBody LotBlockRequest request) {
        return ResponseEntity.ok(quarantineService.block(lotId, request));
    }

    @PatchMapping("/stock/lots/{lotId}/unblock")
    @Audited(module = "INVENTORY", action = "UNBLOCK_LOT", ressource = "stock_lot")
    @Idempotent(operation = "INVENTORY_LOT_UNBLOCK", required = false)
    public ResponseEntity<StockLotResponse> unblock(@PathVariable Long lotId,
                                                    @RequestParam(required = false) String unblockedBy) {
        return ResponseEntity.ok(quarantineService.unblock(lotId, unblockedBy));
    }

    @GetMapping("/stock/lots/immobilised")
    public ResponseEntity<List<StockLotResponse>> immobilised(@RequestParam(required = false) String itemCode,
                                                              @RequestParam(required = false) String locationCode) {
        return ResponseEntity.ok(quarantineService.listImmobilised(itemCode, locationCode));
    }
}
