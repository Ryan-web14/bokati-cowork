package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleResponse;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleSuccessorRequest;
import com.sni.bokaticowork.features.billing.dto.CatalogReferenceDtos.TaxRuleUpdateRequest;
import com.sni.bokaticowork.features.billing.service.implementation.CatalogReferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * Lot C0 : regles de taxe de la facturation. Un taux ne se modifie pas : il se remplace par une
 * nouvelle version datee ({@code POST /{baseCode}/successor}). Gestion reservee au droit
 * BILLING_TAX_RULES.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing/tax-rules")
@RequiredArgsConstructor
public class TaxRuleController {

    private static final String MANAGE = "hasAuthority('BILLING:TAX_RULES')";

    private final CatalogReferenceService references;

    /** Toutes les versions ; {@code includeInactive} pour voir aussi les regles desactivees. */
    @GetMapping
    public ResponseEntity<List<TaxRuleResponse>> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(references.listTaxRules(includeInactive));
    }

    /** Versions d'une regle par son code de base, de la plus ancienne a la plus recente. */
    @GetMapping("/{baseCode}/versions")
    public ResponseEntity<List<TaxRuleResponse>> versions(@PathVariable String baseCode) {
        return ResponseEntity.ok(references.taxRuleVersions(baseCode));
    }

    @PostMapping
    @PreAuthorize(MANAGE)
    public ResponseEntity<TaxRuleResponse> create(@Valid @RequestBody TaxRuleRequest request) {
        return ResponseEntity.ok(references.createTaxRule(request));
    }

    /** Libelles et mentions d'une version ; le taux ne change pas ici. */
    @PutMapping("/{code}")
    @PreAuthorize(MANAGE)
    public ResponseEntity<TaxRuleResponse> update(@PathVariable String code, @Valid @RequestBody TaxRuleUpdateRequest request) {
        return ResponseEntity.ok(references.updateTaxRule(code, request));
    }

    /** Changement de taux a une date : la version en cours s'arrete la veille. */
    @PostMapping("/{baseCode}/successor")
    @PreAuthorize(MANAGE)
    public ResponseEntity<TaxRuleResponse> successor(@PathVariable String baseCode, @Valid @RequestBody TaxRuleSuccessorRequest request) {
        return ResponseEntity.ok(references.successor(baseCode, request));
    }

    @PatchMapping("/{code}/activate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<TaxRuleResponse> activate(@PathVariable String code) {
        return ResponseEntity.ok(references.setTaxRuleActive(code, true));
    }

    @PatchMapping("/{code}/deactivate")
    @PreAuthorize(MANAGE)
    public ResponseEntity<TaxRuleResponse> deactivate(@PathVariable String code) {
        return ResponseEntity.ok(references.setTaxRuleActive(code, false));
    }
}
