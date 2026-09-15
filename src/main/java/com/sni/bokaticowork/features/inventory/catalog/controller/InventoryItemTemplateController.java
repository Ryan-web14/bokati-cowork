package com.sni.bokaticowork.features.inventory.catalog.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemTemplateRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryVariantGenerationRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemTemplateResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryVariantGenerationResponse;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/item-templates")
public class InventoryItemTemplateController {

    private final InventoryItemTemplateService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "CREATE_ITEM_TEMPLATE", ressource = "inventory_item_template")
    @Idempotent(operation = "INVENTORY_ITEM_TEMPLATE_CREATE", required = false)
    public ResponseEntity<InventoryItemTemplateResponse> create(@Valid @RequestBody InventoryItemTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{templateCode}")
    @Audited(module = "INVENTORY", action = "UPDATE_ITEM_TEMPLATE", ressource = "inventory_item_template")
    @Idempotent(operation = "INVENTORY_ITEM_TEMPLATE_UPDATE", required = false)
    public ResponseEntity<InventoryItemTemplateResponse> update(@PathVariable String templateCode,
                                                                @Valid @RequestBody InventoryItemTemplateRequest request) {
        return ResponseEntity.ok(service.update(templateCode, request));
    }

    @GetMapping("/{templateCode}")
    public ResponseEntity<InventoryItemTemplateResponse> get(@PathVariable String templateCode) {
        return ResponseEntity.ok(service.get(templateCode));
    }

    @GetMapping
    public ResponseEntity<List<InventoryItemTemplateResponse>> list(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(service.list(active));
    }

    /**
     * Genere les variantes manquantes. Passer dryRun a vrai pour verifier le volume avant de creer.
     */
    @PostMapping("/{templateCode}/variants")
    @Audited(module = "INVENTORY", action = "GENERATE_ITEM_VARIANTS", ressource = "inventory_item_template")
    @Idempotent(operation = "INVENTORY_ITEM_TEMPLATE_GENERATE", required = false)
    public ResponseEntity<InventoryVariantGenerationResponse> generate(
            @PathVariable String templateCode,
            @RequestBody(required = false) InventoryVariantGenerationRequest request) {
        return ResponseEntity.ok(service.generateVariants(templateCode, request));
    }
}
