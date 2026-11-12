package com.sni.bokaticowork.features.inventory.procurement.suggestion.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.dto.SupplierSuggestionDtos.*;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.model.SupplierCategoryKind;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.service.SupplierCategoryService;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.service.SupplierSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Categories de fournisseurs (referentiel et rattachement des fiches) et suggestion de fournisseurs.
 * Droits du module inventaire : lecture INVENTORY:READ, ecriture INVENTORY:CREATE / UPDATE.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/procurement")
public class SupplierSuggestionController {

    private final SupplierCategoryService categories;
    private final SupplierSuggestionService suggestions;

    // ------------------------------------------------------------------ referentiel des categories fournisseurs

    @GetMapping("/supplier-categories")
    public ResponseEntity<List<RefCategoryView>> categories(@RequestParam(required = false) String q,
                                                            @RequestParam(required = false) Boolean active,
                                                            @RequestParam(required = false) String parentCode) {
        return ResponseEntity.ok(categories.categories(q, active, parentCode));
    }

    @GetMapping("/supplier-categories/{code}")
    public ResponseEntity<RefCategoryView> category(@PathVariable String code) {
        return ResponseEntity.ok(categories.category(code));
    }

    @PostMapping("/supplier-categories")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CATEGORY_CREATE", ressource = "inventory_supplier_category_ref")
    public ResponseEntity<RefCategoryView> createCategory(@Valid @RequestBody RefCategoryRequest request) {
        return ResponseEntity.ok(categories.create(request));
    }

    @PutMapping("/supplier-categories/{code}")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CATEGORY_UPDATE", ressource = "inventory_supplier_category_ref")
    public ResponseEntity<RefCategoryView> updateCategory(@PathVariable String code, @Valid @RequestBody RefCategoryRequest request) {
        return ResponseEntity.ok(categories.update(code, request));
    }

    @PatchMapping("/supplier-categories/{code}/deactivate")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CATEGORY_DEACTIVATE", ressource = "inventory_supplier_category_ref")
    public ResponseEntity<RefCategoryView> deactivateCategory(@PathVariable String code) {
        return ResponseEntity.ok(categories.setActive(code, false));
    }

    @PatchMapping("/supplier-categories/{code}/activate")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CATEGORY_ACTIVATE", ressource = "inventory_supplier_category_ref")
    public ResponseEntity<RefCategoryView> activateCategory(@PathVariable String code) {
        return ResponseEntity.ok(categories.setActive(code, true));
    }

    @DeleteMapping("/supplier-categories/{code}")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CATEGORY_DELETE", ressource = "inventory_supplier_category_ref")
    public ResponseEntity<Void> deleteCategory(@PathVariable String code) {
        categories.delete(code);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ categories d'un fournisseur

    @GetMapping("/suppliers/{supplierCode}/categories")
    public ResponseEntity<List<SupplierCategoryView>> supplierCategories(@PathVariable String supplierCode) {
        return ResponseEntity.ok(categories.supplierCategories(supplierCode));
    }

    @PutMapping("/suppliers/{supplierCode}/categories")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CATEGORIES_REPLACE", ressource = "inventory_supplier_category")
    public ResponseEntity<List<SupplierCategoryView>> replaceSupplierCategories(@PathVariable String supplierCode,
                                                                                @Valid @RequestBody SupplierCategoriesRequest request) {
        return ResponseEntity.ok(categories.replaceSupplierCategories(supplierCode, request.categories()));
    }

    @PostMapping("/suppliers/{supplierCode}/categories")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CATEGORY_ADD", ressource = "inventory_supplier_category")
    public ResponseEntity<List<SupplierCategoryView>> addSupplierCategory(@PathVariable String supplierCode,
                                                                          @Valid @RequestBody CategoryRef request) {
        return ResponseEntity.ok(categories.addSupplierCategory(supplierCode, request));
    }

    @DeleteMapping("/suppliers/{supplierCode}/categories/{kind}/{code}")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CATEGORY_REMOVE", ressource = "inventory_supplier_category")
    public ResponseEntity<List<SupplierCategoryView>> removeSupplierCategory(@PathVariable String supplierCode,
                                                                             @PathVariable SupplierCategoryKind kind,
                                                                             @PathVariable String code) {
        return ResponseEntity.ok(categories.removeSupplierCategory(supplierCode, kind, code));
    }

    // ------------------------------------------------------------------ suggestion

    /**
     * Fournisseurs actifs classes pour une categorie de besoin. A defaut de nature, elle est devinee
     * depuis la designation.
     */
    @GetMapping("/supplier-suggestions")
    public ResponseEntity<SuggestionResult> suggest(@RequestParam(required = false) String natureCode,
                                                    @RequestParam(required = false) String inventoryCategoryCode,
                                                    @RequestParam(required = false) String supplierCategoryCode,
                                                    @RequestParam(required = false) String designation,
                                                    @RequestParam(required = false) String itemCode,
                                                    @RequestParam(required = false) BigDecimal amount,
                                                    @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(suggestions.suggest(new SuggestionQuery(natureCode, inventoryCategoryCode, supplierCategoryCode, designation,
                itemCode, amount, limit)));
    }
}
