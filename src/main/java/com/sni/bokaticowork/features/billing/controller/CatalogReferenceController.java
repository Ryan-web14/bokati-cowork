package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.CategoryRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.CategoryResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.CategoryUpdateRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.MergeRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.ReferencesResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.UnitRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.UnitResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.UnitUpdateRequest;
import com.sni.bokaticowork.features.billing.service.implementation.CatalogReferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Lot C0 : listes de reference du catalogue. Le formulaire d'un article se remplit avec
 * {@code GET /billing/catalog/references} ; les autres routes gerent les familles, sous-familles
 * et unites (droit BILLING_CATALOG_REFERENCES).
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing/catalog")
@RequiredArgsConstructor
public class CatalogReferenceController {

    private static final String MANAGE = "hasAuthority('BILLING:CATALOG_REFERENCES')";

    private final CatalogReferenceService references;

    /** Toutes les listes en un appel : familles et sous-familles, unites, taxes choisissables. */
    @GetMapping("/references")
    public ResponseEntity<ReferencesResponse> references(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(references.references(includeInactive));
    }

    /** Fusion de deux entrees en double : les articles passent sur la cible. */
    @PostMapping("/references/merge")
    @PreAuthorize(MANAGE)
    public ResponseEntity<Map<String, Long>> merge(@Valid @RequestBody MergeRequest request) {
        return ResponseEntity.ok(Map.of("movedItems", references.merge(request)));
    }

    // ------------------------------------------------------------------ familles

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> categories(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(references.listCategories(includeInactive));
    }

    @PostMapping("/categories")
    @PreAuthorize(MANAGE)
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(references.createCategory(request));
    }

    @PutMapping("/categories/{code}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable String code, @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(references.updateCategory(code, null, request));
    }

    @PatchMapping("/categories/{code}/activate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<CategoryResponse> activateCategory(@PathVariable String code) {
        return ResponseEntity.ok(references.setCategoryActive(code, null, true));
    }

    @PatchMapping("/categories/{code}/deactivate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<CategoryResponse> deactivateCategory(@PathVariable String code) {
        return ResponseEntity.ok(references.setCategoryActive(code, null, false));
    }

    @DeleteMapping("/categories/{code}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<Void> deleteCategory(@PathVariable String code) {
        references.deleteCategory(code, null);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ sous-familles

    @GetMapping("/categories/{code}/subcategories")
    public ResponseEntity<List<CategoryResponse>> subcategories(@PathVariable String code,
                                                                @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(references.listCategories(true).stream()
                .filter(family -> family.code().equalsIgnoreCase(code.trim()))
                .findFirst()
                .map(family -> family.subcategories().stream()
                        .filter(sub -> includeInactive || Boolean.TRUE.equals(sub.active()))
                        .toList())
                .orElseThrow(() -> new ResourceNotFoundException("Famille du catalogue introuvable : " + code)));
    }

    @PostMapping("/categories/{code}/subcategories")
    @PreAuthorize(MANAGE)
    public ResponseEntity<CategoryResponse> createSubcategory(@PathVariable String code, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(references.createSubcategory(code, request));
    }

    @PutMapping("/categories/{code}/subcategories/{subCode}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<CategoryResponse> updateSubcategory(@PathVariable String code, @PathVariable String subCode,
                                                              @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(references.updateCategory(code, subCode, request));
    }

    @PatchMapping("/categories/{code}/subcategories/{subCode}/activate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<CategoryResponse> activateSubcategory(@PathVariable String code, @PathVariable String subCode) {
        return ResponseEntity.ok(references.setCategoryActive(code, subCode, true));
    }

    @PatchMapping("/categories/{code}/subcategories/{subCode}/deactivate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<CategoryResponse> deactivateSubcategory(@PathVariable String code, @PathVariable String subCode) {
        return ResponseEntity.ok(references.setCategoryActive(code, subCode, false));
    }

    @DeleteMapping("/categories/{code}/subcategories/{subCode}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<Void> deleteSubcategory(@PathVariable String code, @PathVariable String subCode) {
        references.deleteCategory(code, subCode);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ unites

    @GetMapping("/units")
    public ResponseEntity<List<UnitResponse>> units(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(references.listUnits(includeInactive));
    }

    @PostMapping("/units")
    @PreAuthorize(MANAGE)
    public ResponseEntity<UnitResponse> createUnit(@Valid @RequestBody UnitRequest request) {
        return ResponseEntity.ok(references.createUnit(request));
    }

    @PutMapping("/units/{code}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<UnitResponse> updateUnit(@PathVariable String code, @Valid @RequestBody UnitUpdateRequest request) {
        return ResponseEntity.ok(references.updateUnit(code, request));
    }

    @PatchMapping("/units/{code}/activate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<UnitResponse> activateUnit(@PathVariable String code) {
        return ResponseEntity.ok(references.setUnitActive(code, true));
    }

    @PatchMapping("/units/{code}/deactivate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<UnitResponse> deactivateUnit(@PathVariable String code) {
        return ResponseEntity.ok(references.setUnitActive(code, false));
    }

    @DeleteMapping("/units/{code}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<Void> deleteUnit(@PathVariable String code) {
        references.deleteUnit(code);
        return ResponseEntity.noContent().build();
    }
}
