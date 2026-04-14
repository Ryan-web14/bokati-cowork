package com.sni.bokaticowork.features.inventory.catalog.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryCategoryRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryCategoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/categories")
public class InventoryCategoryController {

    private final InventoryCategoryService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "CREATE_CATEGORY", ressource = "inventory_category")
    @Idempotent(operation = "INVENTORY_CATEGORY_CREATE", required = false)
    public ResponseEntity<InventoryCategoryResponse> create(@Valid @RequestBody InventoryCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{code}")
    @Audited(module = "INVENTORY", action = "UPDATE_CATEGORY", ressource = "inventory_category")
    @Idempotent(operation = "INVENTORY_CATEGORY_UPDATE", required = false)
    public ResponseEntity<InventoryCategoryResponse> update(@PathVariable String code,
                                                            @Valid @RequestBody InventoryCategoryRequest request) {
        return ResponseEntity.ok(service.update(code, request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<InventoryCategoryResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(service.get(code));
    }

    @GetMapping
    public ResponseEntity<List<InventoryCategoryResponse>> list(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.list(active));
    }

    @PatchMapping("/{code}/activate")
    @Audited(module = "INVENTORY", action = "ACTIVATE_CATEGORY", ressource = "inventory_category")
    @Idempotent(operation = "INVENTORY_CATEGORY_ACTIVATE", required = false)
    public ResponseEntity<InventoryCategoryResponse> activate(@PathVariable String code) {
        return ResponseEntity.ok(service.activate(code));
    }

    @PatchMapping("/{code}/deactivate")
    @Audited(module = "INVENTORY", action = "DEACTIVATE_CATEGORY", ressource = "inventory_category")
    @Idempotent(operation = "INVENTORY_CATEGORY_DEACTIVATE", required = false)
    public ResponseEntity<InventoryCategoryResponse> deactivate(@PathVariable String code) {
        return ResponseEntity.ok(service.deactivate(code));
    }
}
