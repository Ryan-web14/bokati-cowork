package com.sni.bokaticowork.features.inventory.intelligence.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryAlertResponse;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/alerts")
public class InventoryAlertController {

    private final InventoryAlertService service;

    @GetMapping
    public ResponseEntity<Page<InventoryAlertResponse>> list(@RequestParam(required = false) InventoryAlertStatus status,
                                                             @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.list(status, pageable));
    }

    @PatchMapping("/{alertCode}/acknowledge")
    @Audited(module = "INVENTORY", action = "ACKNOWLEDGE_ALERT", ressource = "inventory_alert")
    @Idempotent(operation = "INVENTORY_ALERT_ACKNOWLEDGE", required = false)
    public ResponseEntity<InventoryAlertResponse> acknowledge(@PathVariable String alertCode) {
        return ResponseEntity.ok(service.acknowledge(alertCode));
    }

    @PatchMapping("/{alertCode}/resolve")
    @Audited(module = "INVENTORY", action = "RESOLVE_ALERT", ressource = "inventory_alert")
    @Idempotent(operation = "INVENTORY_ALERT_RESOLVE", required = false)
    public ResponseEntity<InventoryAlertResponse> resolve(@PathVariable String alertCode) {
        return ResponseEntity.ok(service.resolve(alertCode));
    }

    @PatchMapping("/{alertCode}/dismiss")
    @Audited(module = "INVENTORY", action = "DISMISS_ALERT", ressource = "inventory_alert")
    @Idempotent(operation = "INVENTORY_ALERT_DISMISS", required = false)
    public ResponseEntity<InventoryAlertResponse> dismiss(@PathVariable String alertCode) {
        return ResponseEntity.ok(service.dismiss(alertCode));
    }
}
