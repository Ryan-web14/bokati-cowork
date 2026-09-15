package com.sni.bokaticowork.features.inventory.catalog.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryBarcodeRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemLifecycleRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRevisionRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemSubstituteRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemTranslationRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryPackagingRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryBarcodeResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemPriceHistoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemRevisionHistoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemSubstituteResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemTranslationResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryPackagingResponse;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryIdentificationService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemRelationService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/items")
public class InventoryItemController {

    private final InventoryItemService service;
    private final InventoryIdentificationService identificationService;
    private final InventoryItemRelationService relationService;

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

    @DeleteMapping("/{itemCode}")
    @Audited(module = "INVENTORY", action = "DELETE_ITEM", ressource = "inventory_item")
    @Idempotent(operation = "INVENTORY_ITEM_DELETE", required = false)
    public ResponseEntity<Void> delete(@PathVariable String itemCode) {
        service.delete(itemCode);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change le statut de cycle de vie. Plus expressif que activate et deactivate, qui restent
     * disponibles pour les clients existants.
     */
    @PatchMapping("/{itemCode}/lifecycle")
    @Audited(module = "INVENTORY", action = "CHANGE_ITEM_LIFECYCLE", ressource = "inventory_item")
    @Idempotent(operation = "INVENTORY_ITEM_LIFECYCLE", required = false)
    public ResponseEntity<InventoryItemResponse> changeLifecycle(@PathVariable String itemCode,
                                                                 @Valid @RequestBody InventoryItemLifecycleRequest request) {
        return ResponseEntity.ok(service.changeLifecycle(itemCode, request));
    }

    @PatchMapping("/{itemCode}/revision")
    @Audited(module = "INVENTORY", action = "CHANGE_ITEM_REVISION", ressource = "inventory_item")
    @Idempotent(operation = "INVENTORY_ITEM_REVISION", required = false)
    public ResponseEntity<InventoryItemResponse> changeRevision(@PathVariable String itemCode,
                                                                @Valid @RequestBody InventoryItemRevisionRequest request) {
        return ResponseEntity.ok(service.changeRevision(itemCode, request));
    }

    @GetMapping("/{itemCode}/price-history")
    public ResponseEntity<Page<InventoryItemPriceHistoryResponse>> priceHistory(
            @PathVariable String itemCode,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.priceHistory(itemCode, pageable));
    }

    @GetMapping("/{itemCode}/revision-history")
    public ResponseEntity<List<InventoryItemRevisionHistoryResponse>> revisionHistory(@PathVariable String itemCode) {
        return ResponseEntity.ok(service.revisionHistory(itemCode));
    }

    // Codes-barres

    @PostMapping("/{itemCode}/barcodes")
    @Audited(module = "INVENTORY", action = "ADD_ITEM_BARCODE", ressource = "inventory_barcode")
    @Idempotent(operation = "INVENTORY_ITEM_BARCODE_ADD", required = false)
    public ResponseEntity<InventoryBarcodeResponse> addBarcode(@PathVariable String itemCode,
                                                               @Valid @RequestBody InventoryBarcodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(identificationService.addBarcode(itemCode, request));
    }

    @GetMapping("/{itemCode}/barcodes")
    public ResponseEntity<List<InventoryBarcodeResponse>> listBarcodes(@PathVariable String itemCode) {
        return ResponseEntity.ok(identificationService.listBarcodes(itemCode));
    }

    @PatchMapping("/barcodes/{barcodeId}/primary")
    @Audited(module = "INVENTORY", action = "SET_PRIMARY_BARCODE", ressource = "inventory_barcode")
    @Idempotent(operation = "INVENTORY_ITEM_BARCODE_PRIMARY", required = false)
    public ResponseEntity<InventoryBarcodeResponse> markPrimaryBarcode(@PathVariable Long barcodeId) {
        return ResponseEntity.ok(identificationService.markPrimary(barcodeId));
    }

    @DeleteMapping("/barcodes/{barcodeId}")
    @Audited(module = "INVENTORY", action = "DELETE_ITEM_BARCODE", ressource = "inventory_barcode")
    public ResponseEntity<Void> deleteBarcode(@PathVariable Long barcodeId) {
        identificationService.deleteBarcode(barcodeId);
        return ResponseEntity.noContent().build();
    }

    // Conditionnements

    @PostMapping("/{itemCode}/packagings")
    @Audited(module = "INVENTORY", action = "ADD_ITEM_PACKAGING", ressource = "inventory_packaging")
    @Idempotent(operation = "INVENTORY_ITEM_PACKAGING_ADD", required = false)
    public ResponseEntity<InventoryPackagingResponse> addPackaging(@PathVariable String itemCode,
                                                                    @Valid @RequestBody InventoryPackagingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(identificationService.addPackaging(itemCode, request));
    }

    @GetMapping("/{itemCode}/packagings")
    public ResponseEntity<List<InventoryPackagingResponse>> listPackagings(@PathVariable String itemCode) {
        return ResponseEntity.ok(identificationService.listPackagings(itemCode));
    }

    @DeleteMapping("/packagings/{packagingId}")
    @Audited(module = "INVENTORY", action = "DELETE_ITEM_PACKAGING", ressource = "inventory_packaging")
    public ResponseEntity<Void> deletePackaging(@PathVariable Long packagingId) {
        identificationService.deletePackaging(packagingId);
        return ResponseEntity.noContent().build();
    }

    // Substituts

    @PostMapping("/{itemCode}/substitutes")
    @Audited(module = "INVENTORY", action = "ADD_ITEM_SUBSTITUTE", ressource = "inventory_item_substitute")
    @Idempotent(operation = "INVENTORY_ITEM_SUBSTITUTE_ADD", required = false)
    public ResponseEntity<InventoryItemSubstituteResponse> addSubstitute(@PathVariable String itemCode,
                                                                         @Valid @RequestBody InventoryItemSubstituteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(relationService.addSubstitute(itemCode, request));
    }

    @GetMapping("/{itemCode}/substitutes")
    public ResponseEntity<List<InventoryItemSubstituteResponse>> listSubstitutes(@PathVariable String itemCode) {
        return ResponseEntity.ok(relationService.listSubstitutes(itemCode));
    }

    @DeleteMapping("/substitutes/{substituteId}")
    @Audited(module = "INVENTORY", action = "DELETE_ITEM_SUBSTITUTE", ressource = "inventory_item_substitute")
    public ResponseEntity<Void> deleteSubstitute(@PathVariable Long substituteId) {
        relationService.deleteSubstitute(substituteId);
        return ResponseEntity.noContent().build();
    }

    // Traductions

    @PutMapping("/{itemCode}/translations")
    @Audited(module = "INVENTORY", action = "UPSERT_ITEM_TRANSLATION", ressource = "inventory_item_translation")
    @Idempotent(operation = "INVENTORY_ITEM_TRANSLATION_UPSERT", required = false)
    public ResponseEntity<InventoryItemTranslationResponse> upsertTranslation(@PathVariable String itemCode,
                                                                              @Valid @RequestBody InventoryItemTranslationRequest request) {
        return ResponseEntity.ok(relationService.upsertTranslation(itemCode, request));
    }

    @GetMapping("/{itemCode}/translations")
    public ResponseEntity<List<InventoryItemTranslationResponse>> listTranslations(@PathVariable String itemCode) {
        return ResponseEntity.ok(relationService.listTranslations(itemCode));
    }

    @DeleteMapping("/translations/{translationId}")
    @Audited(module = "INVENTORY", action = "DELETE_ITEM_TRANSLATION", ressource = "inventory_item_translation")
    public ResponseEntity<Void> deleteTranslation(@PathVariable Long translationId) {
        relationService.deleteTranslation(translationId);
        return ResponseEntity.noContent().build();
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
