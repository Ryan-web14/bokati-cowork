package com.sni.bokaticowork.features.inventory.intelligence.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.intelligence.dto.request.InventoryReorderRuleRequest;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryReorderRuleResponse;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.ReorderSuggestionResponse;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionSeverity;
import com.sni.bokaticowork.features.inventory.intelligence.service.interfaces.InventoryReorderRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/reorder-rules")
public class InventoryReorderRuleController {

    private final InventoryReorderRuleService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "UPSERT_REORDER_RULE", ressource = "inventory_reorder_rule")
    @Idempotent(operation = "INVENTORY_REORDER_RULE_UPSERT", required = false)
    public ResponseEntity<InventoryReorderRuleResponse> createOrUpdate(@Valid @RequestBody InventoryReorderRuleRequest request) {
        return ResponseEntity.ok(service.createOrUpdate(request));
    }

    @GetMapping
    public ResponseEntity<List<InventoryReorderRuleResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<ReorderSuggestionResponse>> suggestions(
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) ReorderSuggestionSeverity severity) {
        return ResponseEntity.ok(service.suggestions(itemCode, locationCode, severity));
    }
}
