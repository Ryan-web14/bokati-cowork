package com.sni.bokaticowork.features.inventory.catalog.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryUnitRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryUnitResponse;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/units")
public class InventoryUnitController {

    private final InventoryUnitService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "CREATE_UNIT", ressource = "inventory_unit")
    @Idempotent(operation = "INVENTORY_UNIT_CREATE", required = false)
    public ResponseEntity<InventoryUnitResponse> create(@Valid @RequestBody InventoryUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{code}")
    @Audited(module = "INVENTORY", action = "UPDATE_UNIT", ressource = "inventory_unit")
    @Idempotent(operation = "INVENTORY_UNIT_UPDATE", required = false)
    public ResponseEntity<InventoryUnitResponse> update(@PathVariable String code,
                                                        @Valid @RequestBody InventoryUnitRequest request) {
        return ResponseEntity.ok(service.update(code, request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<InventoryUnitResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(service.get(code));
    }

    @GetMapping
    public ResponseEntity<List<InventoryUnitResponse>> list(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.list(active));
    }

    @PatchMapping("/{code}/activate")
    @Audited(module = "INVENTORY", action = "ACTIVATE_UNIT", ressource = "inventory_unit")
    @Idempotent(operation = "INVENTORY_UNIT_ACTIVATE", required = false)
    public ResponseEntity<InventoryUnitResponse> activate(@PathVariable String code) {
        return ResponseEntity.ok(service.activate(code));
    }

    @PatchMapping("/{code}/deactivate")
    @Audited(module = "INVENTORY", action = "DEACTIVATE_UNIT", ressource = "inventory_unit")
    @Idempotent(operation = "INVENTORY_UNIT_DEACTIVATE", required = false)
    public ResponseEntity<InventoryUnitResponse> deactivate(@PathVariable String code) {
        return ResponseEntity.ok(service.deactivate(code));
    }
}
