package com.sni.bokaticowork.features.inventory.control.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.control.dto.request.InventoryCountCreateRequest;
import com.sni.bokaticowork.features.inventory.control.dto.request.InventoryCountLineRequest;
import com.sni.bokaticowork.features.inventory.control.dto.response.InventoryCountResponse;
import com.sni.bokaticowork.features.inventory.control.enums.InventoryCountStatus;
import com.sni.bokaticowork.features.inventory.control.service.InventoryCountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/counts")
public class InventoryCountController {

    private final InventoryCountService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "COUNT_CREATE", ressource = "inventory_count")
    @Idempotent(operation = "INVENTORY_COUNT_CREATE", required = false)
    public ResponseEntity<InventoryCountResponse> create(@Valid @RequestBody InventoryCountCreateRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PatchMapping("/{countCode}/start")
    public ResponseEntity<InventoryCountResponse> start(@PathVariable String countCode) {
        return ResponseEntity.ok(service.start(countCode));
    }

    @PutMapping("/{countCode}/lines")
    @Audited(module = "INVENTORY", action = "COUNT_LINE_RECORD", ressource = "inventory_count")
    @Idempotent(operation = "INVENTORY_COUNT_LINE_RECORD", required = false)
    public ResponseEntity<InventoryCountResponse> recordLine(@PathVariable String countCode,
                                                             @Valid @RequestBody InventoryCountLineRequest request) {
        return ResponseEntity.ok(service.recordLine(countCode, request));
    }

    @PatchMapping("/{countCode}/review")
    public ResponseEntity<InventoryCountResponse> review(@PathVariable String countCode) {
        return ResponseEntity.ok(service.sendToReview(countCode));
    }

    @PatchMapping("/{countCode}/validate")
    @Audited(module = "INVENTORY", action = "COUNT_VALIDATE", ressource = "inventory_count")
    @Idempotent(operation = "INVENTORY_COUNT_VALIDATE", required = false)
    public ResponseEntity<InventoryCountResponse> validate(@PathVariable String countCode,
                                                           @RequestParam(required = false) String performedBy) {
        return ResponseEntity.ok(service.validate(countCode, performedBy));
    }

    @PatchMapping("/{countCode}/cancel")
    public ResponseEntity<InventoryCountResponse> cancel(@PathVariable String countCode) {
        return ResponseEntity.ok(service.cancel(countCode));
    }

    @GetMapping("/{countCode}")
    public ResponseEntity<InventoryCountResponse> get(@PathVariable String countCode) {
        return ResponseEntity.ok(service.get(countCode));
    }

    @GetMapping
    public ResponseEntity<Page<InventoryCountResponse>> list(@RequestParam(required = false) InventoryCountStatus status,
                                                             @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.list(status, pageable));
    }
}
