package com.sni.bokaticowork.features.inventory.asset.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetMaintenanceCompleteRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetMaintenanceRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetMaintenanceResponse;
import com.sni.bokaticowork.features.inventory.asset.service.interfaces.AssetMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory")
public class AssetMaintenanceController {

    private final AssetMaintenanceService service;

    @PostMapping("/assets/{assetCode}/maintenances")
    @Audited(module = "INVENTORY", action = "PLAN_ASSET_MAINTENANCE", ressource = "asset_maintenance")
    @Idempotent(operation = "INVENTORY_ASSET_MAINTENANCE_PLAN", required = false)
    public ResponseEntity<AssetMaintenanceResponse> plan(@PathVariable String assetCode,
                                                         @Valid @RequestBody AssetMaintenanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.plan(assetCode, request));
    }

    @GetMapping("/assets/{assetCode}/maintenances")
    public ResponseEntity<List<AssetMaintenanceResponse>> list(@PathVariable String assetCode) {
        return ResponseEntity.ok(service.list(assetCode));
    }

    @PatchMapping("/asset-maintenances/{maintenanceCode}/start")
    @Audited(module = "INVENTORY", action = "START_ASSET_MAINTENANCE", ressource = "asset_maintenance")
    @Idempotent(operation = "INVENTORY_ASSET_MAINTENANCE_START", required = false)
    public ResponseEntity<AssetMaintenanceResponse> start(@PathVariable String maintenanceCode) {
        return ResponseEntity.ok(service.start(maintenanceCode));
    }

    @PatchMapping("/asset-maintenances/{maintenanceCode}/complete")
    @Audited(module = "INVENTORY", action = "COMPLETE_ASSET_MAINTENANCE", ressource = "asset_maintenance")
    @Idempotent(operation = "INVENTORY_ASSET_MAINTENANCE_COMPLETE", required = false)
    public ResponseEntity<AssetMaintenanceResponse> complete(@PathVariable String maintenanceCode,
                                                             @RequestBody AssetMaintenanceCompleteRequest request) {
        return ResponseEntity.ok(service.complete(maintenanceCode, request));
    }

    @PatchMapping("/asset-maintenances/{maintenanceCode}/cancel")
    @Audited(module = "INVENTORY", action = "CANCEL_ASSET_MAINTENANCE", ressource = "asset_maintenance")
    @Idempotent(operation = "INVENTORY_ASSET_MAINTENANCE_CANCEL", required = false)
    public ResponseEntity<AssetMaintenanceResponse> cancel(@PathVariable String maintenanceCode) {
        return ResponseEntity.ok(service.cancel(maintenanceCode));
    }
}
