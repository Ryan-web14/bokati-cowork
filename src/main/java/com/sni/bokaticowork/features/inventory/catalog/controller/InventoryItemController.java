package com.sni.bokaticowork.features.inventory.catalog.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/items")
public class InventoryItemController {

    private final InventoryItemService service;

    @PostMapping
    @Audited(module = "INVENTORY", action = "CREATE_ITEM", ressource = "inventory_item")
    @Idempotent(operation = "INVENTORY_ITEM_CREATE", required = false)
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{itemCode}")
    @Audited(module = "INVENTORY", action = "UPDATE_ITEM", ressource = "inventory_item")
    @Idempotent(operation = "INVENTORY_ITEM_UPDATE", required = false)
    public ResponseEntity<InventoryItemResponse> update(@PathVariable String itemCode,
                                                        @Valid @RequestBody InventoryItemRequest request) {
        return ResponseEntity.ok(service.update(itemCode, request));
    }

    @GetMapping("/{itemCode}")
    public ResponseEntity<InventoryItemResponse> get(@PathVariable String itemCode) {
        return ResponseEntity.ok(service.get(itemCode));
    }

    @GetMapping
    public ResponseEntity<Page<InventoryItemResponse>> search(@RequestParam(required = false, name = "q") String q,
                                                              @RequestParam(required = false, name = "query") String query,
                                                              @RequestParam(required = false, name = "searchText") String searchText,
                                                              @RequestParam(required = false, name = "name") String name,
                                                              @RequestParam(required = false) String categoryCode,
                                                              @RequestParam(required = false) InventoryItemType itemType,
                                                              @RequestParam(required = false) Boolean active,
                                                              @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.search(firstSearchText(q, query, searchText, name), categoryCode, itemType, active, pageable));
    }

    @PatchMapping("/{itemCode}/activate")
    @Audited(module = "INVENTORY", action = "ACTIVATE_ITEM", ressource = "inventory_item")
    @Idempotent(operation = "INVENTORY_ITEM_ACTIVATE", required = false)
    public ResponseEntity<InventoryItemResponse> activate(@PathVariable String itemCode) {
        return ResponseEntity.ok(service.activate(itemCode));
    }

    @PatchMapping("/{itemCode}/deactivate")
    @Audited(module = "INVENTORY", action = "DEACTIVATE_ITEM", ressource = "inventory_item")
    @Idempotent(operation = "INVENTORY_ITEM_DEACTIVATE", required = false)
    public ResponseEntity<InventoryItemResponse> deactivate(@PathVariable String itemCode) {
        return ResponseEntity.ok(service.deactivate(itemCode));
    }

    private String firstSearchText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
