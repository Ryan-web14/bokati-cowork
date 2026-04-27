package com.sni.bokaticowork.features.subscription.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateEntitlementDefinitionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanVersionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.UpdatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementDefinitionResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanVersionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.ConsumptionMode;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementResetPolicy;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementType;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.EntitlementDefinitionSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PlanSearchCriteria;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionCatalogService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions")
@RequiredArgsConstructor
public class SubscriptionCatalogController {

    private final SubscriptionCatalogService catalogService;

    @PostMapping("/plans")
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createPlan(request));
    }

    @PutMapping("/plans/{planCode}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable String planCode,
                                                   @Valid @RequestBody UpdatePlanRequest request) {
        return ResponseEntity.ok(catalogService.updatePlan(planCode, request));
    }

    @PostMapping("/plans/{planCode}/versions")
    public ResponseEntity<PlanVersionResponse> createVersion(@PathVariable String planCode,
                                                             @Valid @RequestBody CreatePlanVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createVersion(planCode, request));
    }

    @GetMapping("/plans/{planCode}/versions")
    public ResponseEntity<List<PlanVersionResponse>> listVersions(@PathVariable String planCode) {
        return ResponseEntity.ok(catalogService.listVersions(planCode));
    }

    @PatchMapping("/plans/{planCode}/versions/{versionId}/publish")
    public ResponseEntity<PlanVersionResponse> publishVersion(@PathVariable String planCode, @PathVariable Long versionId) {
        return ResponseEntity.ok(catalogService.publishVersion(planCode, versionId));
    }

    @GetMapping("/plans/{planCode}/versions/{versionId}")
    public ResponseEntity<PlanVersionResponse> getVersion(@PathVariable String planCode, @PathVariable Long versionId) {
        return ResponseEntity.ok(catalogService.getVersion(planCode, versionId));
    }

    @GetMapping("/plans/{planCode}")
    public ResponseEntity<PlanResponse> getPlan(@PathVariable String planCode) {
        return ResponseEntity.ok(catalogService.getPlan(planCode));
    }

    @DeleteMapping("/plans/{planCode}")
    public ResponseEntity<Void> deletePlan(@PathVariable String planCode) {
        catalogService.deletePlan(planCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/plans")
    public ResponseEntity<PaginatedResponse<PlanResponse>> listPlans(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) PlanType planType,
            @RequestParam(required = false) TargetAudience targetAudience,
            @RequestParam(required = false) PlanStatus status,
            @RequestParam(required = false) Boolean visible,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(catalogService.listPlans(
                PlanSearchCriteria.builder()
                        .code(code)
                        .name(name)
                        .planType(planType)
                        .targetAudience(targetAudience)
                        .status(status)
                        .visible(visible)
                        .build(),
                pageable
        ));
    }

    @PostMapping("/entitlement-definitions")
    public ResponseEntity<EntitlementDefinitionResponse> createEntitlementDefinition(
            @Valid @RequestBody CreateEntitlementDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createEntitlementDefinition(request));
    }

    @PutMapping("/entitlement-definitions/{code}")
    public ResponseEntity<EntitlementDefinitionResponse> updateEntitlementDefinition(
            @PathVariable String code,
            @Valid @RequestBody CreateEntitlementDefinitionRequest request) {
        return ResponseEntity.ok(catalogService.updateEntitlementDefinition(code, request));
    }

    @GetMapping("/entitlement-definitions/{code}")
    public ResponseEntity<EntitlementDefinitionResponse> getEntitlementDefinition(@PathVariable String code) {
        return ResponseEntity.ok(catalogService.getEntitlementDefinition(code));
    }

    @GetMapping("/entitlement-definitions/search/basic")
    public ResponseEntity<List<EntitlementDefinitionResponse>> basicSearchEntitlementDefinitions(@RequestParam String query) {
        return ResponseEntity.ok(catalogService.basicSearchEntitlementDefinitions(query));
    }

    @GetMapping("/entitlement-definitions")
    public ResponseEntity<PaginatedResponse<EntitlementDefinitionResponse>> listEntitlementDefinitions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) EntitlementType entitlementType,
            @RequestParam(required = false) EntitlementUnit unit,
            @RequestParam(required = false) ConsumptionMode consumptionMode,
            @RequestParam(required = false) EntitlementResetPolicy resetPolicy,
            @RequestParam(required = false) String resourceTypeCode,
            @RequestParam(required = false) String resourceGroupCode,
            @RequestParam(required = false) Boolean stackable,
            @RequestParam(required = false) Boolean transferable,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(catalogService.listEntitlementDefinitions(
                EntitlementDefinitionSearchCriteria.builder()
                        .query(query)
                        .code(code)
                        .name(name)
                        .entitlementType(entitlementType)
                        .unit(unit)
                        .consumptionMode(consumptionMode)
                        .resetPolicy(resetPolicy)
                        .resourceTypeCode(resourceTypeCode)
                        .resourceGroupCode(resourceGroupCode)
                        .stackable(stackable)
                        .transferable(transferable)
                        .active(active)
                        .build(),
                pageable
        ));
    }
}
