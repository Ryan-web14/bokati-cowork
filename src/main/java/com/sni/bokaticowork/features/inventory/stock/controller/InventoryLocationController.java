package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryLocationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryLocationResponse;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/locations")
public class InventoryLocationController {

    private final InventoryLocationService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "CREATE_LOCATION", ressource = "inventory_location")
    @Idempotent(operation = "INVENTORY_LOCATION_CREATE", required = false)
    public ResponseEntity<InventoryLocationResponse> create(@Valid @RequestBody InventoryLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{locationCode}")
    @Audited(module = "INVENTORY", action = "UPDATE_LOCATION", ressource = "inventory_location")
    @Idempotent(operation = "INVENTORY_LOCATION_UPDATE", required = false)
    public ResponseEntity<InventoryLocationResponse> update(@PathVariable String locationCode,
                                                            @Valid @RequestBody InventoryLocationRequest request) {
        return ResponseEntity.ok(service.update(locationCode, request));
    }

    @GetMapping("/{locationCode}")
    public ResponseEntity<InventoryLocationResponse> get(@PathVariable String locationCode) {
        return ResponseEntity.ok(service.get(locationCode));
    }

    @GetMapping
    public ResponseEntity<List<InventoryLocationResponse>> list(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.list(active));
    }

    @PatchMapping("/{locationCode}/activate")
    @Audited(module = "INVENTORY", action = "ACTIVATE_LOCATION", ressource = "inventory_location")
    @Idempotent(operation = "INVENTORY_LOCATION_ACTIVATE", required = false)
    public ResponseEntity<InventoryLocationResponse> activate(@PathVariable String locationCode) {
        return ResponseEntity.ok(service.activate(locationCode));
    }

    @PatchMapping("/{locationCode}/deactivate")
    @Audited(module = "INVENTORY", action = "DEACTIVATE_LOCATION", ressource = "inventory_location")
    @Idempotent(operation = "INVENTORY_LOCATION_DEACTIVATE", required = false)
    public ResponseEntity<InventoryLocationResponse> deactivate(@PathVariable String locationCode) {
        return ResponseEntity.ok(service.deactivate(locationCode));
    }
}
