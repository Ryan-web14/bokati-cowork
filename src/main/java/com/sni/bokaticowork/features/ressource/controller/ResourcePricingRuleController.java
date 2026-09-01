package com.sni.bokaticowork.features.ressource.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.ressource.dto.request.BulkCreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.BulkResourceOperationResponse;
import com.sni.bokaticowork.features.ressource.dto.response.PricingRuleConflictPreviewResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePricingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping(ApiPath.V1 + "/resource-pricing-rules")
@RequiredArgsConstructor
public class ResourcePricingRuleController {

    private final ResourcePricingRuleService resourcePricingRuleService;

    @PostMapping
    @Audited(module = "RESOURCE", action = "CREATE_PRICING_RULE", ressource = "resource_pricing_rule")
    @Idempotent(operation = "RESOURCE_PRICING_RULE_CREATE")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateResourcePricingRuleRequest request) {
        resourcePricingRuleService.createPricingRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Pose la meme regle sur plusieurs ressources · compte rendu par ressource. */
    @PostMapping("/bulk")
    public ResponseEntity<BulkResourceOperationResponse> createBulk(
            @Valid @RequestBody BulkCreateResourcePricingRuleRequest request) {
        return ResponseEntity.ok(resourcePricingRuleService.createPricingRuleBulk(request));
    }

    /**
     * Montre, sans rien ecrire, ce que la regle viendrait concurrencer.
     *
     * <p>A appeler avant la creation groupee : poser une grille sur vingt ressources rend l'erreur
     * aussi triviale que la saisie. Un chevauchement est normal, arbitre par priorite · c'est le
     * doublon strict qui merite un regard.
     */
    @PostMapping("/bulk/preview")
    public ResponseEntity<PricingRuleConflictPreviewResponse> previewBulk(
            @Valid @RequestBody BulkCreateResourcePricingRuleRequest request) {
        return ResponseEntity.ok(resourcePricingRuleService.previewPricingRuleBulk(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourcePricingRuleResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(resourcePricingRuleService.getPricingRule(id));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ResourcePricingRuleResponse>> list(
            @RequestParam(required = false) String resourceCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (resourceCode != null) {
            return ResponseEntity.ok(resourcePricingRuleService.listByResource(resourceCode, pageable));
        }
        return ResponseEntity.ok(resourcePricingRuleService.list(pageable));
    }

    @GetMapping("/quote")
    public ResponseEntity<ResourcePriceQuoteResponse> quote(
            @RequestParam String resourceCode,
            @RequestParam String bookingUnit,
            @RequestParam LocalDateTime startedAt,
            @RequestParam LocalDateTime endedAt) {
        return ResponseEntity.ok(resourcePricingRuleService.quote(resourceCode, bookingUnit, startedAt, endedAt));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @Audited(module = "RESOURCE", action = "UPDATE_PRICING_RULE", ressource = "resource_pricing_rule")
    public ResponseEntity<ResourcePricingRuleResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateResourcePricingRuleRequest request) {
        return ResponseEntity.ok(resourcePricingRuleService.updatePricingRule(id, request));
    }

    @PatchMapping("/{id}/active")
    @Audited(module = "RESOURCE", action = "UPDATE_PRICING_RULE_ACTIVE", ressource = "resource_pricing_rule")
    @Idempotent(operation = "RESOURCE_PRICING_RULE_UPDATE_ACTIVE", requestBodyArgIndex = -1)
    public ResponseEntity<Void> updateActive(@PathVariable Long id, @RequestParam Boolean active) {
        resourcePricingRuleService.updateActive(id, active);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Audited(module = "RESOURCE", action = "DELETE_PRICING_RULE", ressource = "resource_pricing_rule")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourcePricingRuleService.deletePricingRule(id);
        return ResponseEntity.noContent().build();
    }
}
