package com.sni.bokaticowork.features.subscription.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.dto.PassPlanDtos.*;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassPurchaseRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassPlanService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/pass-plans")
@RequiredArgsConstructor
public class PassPlanController {

    private final PassPlanService passPlanService;
    private final PassService passService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<PassPlanResponse> createPlan(@Valid @RequestBody CreatePassPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passPlanService.createPlan(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')")
    public ResponseEntity<PaginatedResponse<PassPlanResponse>> searchPlans(
            @RequestParam(required = false) PassType passType,
            @RequestParam(required = false) PlanStatus status,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(passPlanService.searchPlans(passType, status, searchText, pageable));
    }

    @GetMapping("/{planCode}")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')")
    public ResponseEntity<PassPlanResponse> getPlan(@PathVariable String planCode) {
        return ResponseEntity.ok(passPlanService.getPlan(planCode));
    }

    @PatchMapping("/{planCode}")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<PassPlanResponse> updatePlan(@PathVariable String planCode,
                                                        @Valid @RequestBody UpdatePassPlanRequest request) {
        return ResponseEntity.ok(passPlanService.updatePlan(planCode, request));
    }

    @PatchMapping("/{planCode}/publish")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<PassPlanResponse> publishPlan(@PathVariable String planCode) {
        return ResponseEntity.ok(passPlanService.publishPlan(planCode));
    }

    @PatchMapping("/{planCode}/archive")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<PassPlanResponse> archivePlan(@PathVariable String planCode) {
        return ResponseEntity.ok(passPlanService.archivePlan(planCode));
    }

    // ── Versions ─────────────────────────────────────────────────

    @PostMapping("/{planCode}/versions")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<PassPlanVersionResponse> createVersion(@PathVariable String planCode,
                                                                  @Valid @RequestBody CreatePassPlanVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passPlanService.createVersion(planCode, request));
    }

    @GetMapping("/{planCode}/versions")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')")
    public ResponseEntity<List<PassPlanVersionResponse>> listVersions(@PathVariable String planCode) {
        return ResponseEntity.ok(passPlanService.listVersions(planCode));
    }

    @PatchMapping("/{planCode}/versions/{versionCode}/publish")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<PassPlanVersionResponse> publishVersion(@PathVariable String planCode,
                                                                    @PathVariable String versionCode) {
        return ResponseEntity.ok(passPlanService.publishVersion(versionCode));
    }

    // ── Prices ───────────────────────────────────────────────────

    @PostMapping("/versions/{versionCode}/prices")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<PassPlanPriceResponse> setPrice(@PathVariable String versionCode,
                                                           @Valid @RequestBody SetPassPlanPriceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passPlanService.setPrice(versionCode, request));
    }

    @GetMapping("/versions/{versionCode}/prices")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')")
    public ResponseEntity<List<PassPlanPriceResponse>> listPrices(@PathVariable String versionCode) {
        return ResponseEntity.ok(passPlanService.listPrices(versionCode));
    }

    // ── Entitlements ─────────────────────────────────────────────

    @PostMapping("/versions/{versionCode}/entitlements")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<PassPlanEntitlementResponse> addEntitlement(@PathVariable String versionCode,
                                                                       @Valid @RequestBody AddPassPlanEntitlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passPlanService.addEntitlement(versionCode, request));
    }

    @DeleteMapping("/versions/{versionCode}/entitlements/{entitlementCode}")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:WRITE','PASS_PLAN_WRITE')")
    public ResponseEntity<Void> removeEntitlement(@PathVariable String versionCode, @PathVariable String entitlementCode) {
        passPlanService.removeEntitlement(versionCode, entitlementCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/versions/{versionCode}/entitlements")
    @PreAuthorize("hasAnyAuthority('PASS_PLAN:READ','PASS_PLAN_READ')")
    public ResponseEntity<List<PassPlanEntitlementResponse>> listEntitlements(@PathVariable String versionCode) {
        return ResponseEntity.ok(passPlanService.listEntitlements(versionCode));
    }

    // ── Purchase ─────────────────────────────────────────────────

    @PostMapping("/{planCode}/purchase")
    @PreAuthorize("hasAnyAuthority('PASS:WRITE','PASS_WRITE')")
    public ResponseEntity<PassResponse> purchase(@PathVariable String planCode,
                                                  @Valid @RequestBody CreatePassPurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passService.purchase(planCode, request));
    }
}
