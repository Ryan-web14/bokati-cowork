package com.sni.bokaticowork.features.subscription.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementOperationResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.EntitlementGrantSearchCriteria;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(ApiPath.V1)
@RequiredArgsConstructor
public class EntitlementController {

    private final EntitlementService entitlementService;

    @GetMapping("/entitlements/grants")
    public ResponseEntity<PaginatedResponse<EntitlementGrantResponse>> listGrants(
            @RequestParam(required = false) SubscriberType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String entitlementCode,
            @RequestParam(required = false) EntitlementGrantStatus status,
            @RequestParam(required = false) Instant validAt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(entitlementService.list(
                EntitlementGrantSearchCriteria.builder()
                        .ownerType(ownerType)
                        .ownerCode(ownerCode)
                        .entitlementCode(entitlementCode)
                        .status(status)
                        .validAt(validAt)
                        .build(),
                pageable
        ));
    }

    @GetMapping("/entitlements/balances")
    public ResponseEntity<java.util.List<EntitlementGrantResponse>> balances(@RequestParam SubscriberType ownerType,
                                                                             @RequestParam String ownerCode) {
        return ResponseEntity.ok(entitlementService.balances(ownerType, ownerCode));
    }

    @PostMapping("/internal/entitlements/check")
    public ResponseEntity<EntitlementOperationResponse> check(@Valid @RequestBody EntitlementOperationRequest request) {
        return ResponseEntity.ok(entitlementService.check(request));
    }

    @PostMapping("/internal/entitlements/reserve")
    public ResponseEntity<EntitlementOperationResponse> reserve(@Valid @RequestBody EntitlementOperationRequest request) {
        return ResponseEntity.ok(entitlementService.reserve(request));
    }

    @PostMapping("/internal/entitlements/consume")
    public ResponseEntity<EntitlementOperationResponse> consume(@Valid @RequestBody EntitlementOperationRequest request) {
        return ResponseEntity.ok(entitlementService.consume(request));
    }

    @PostMapping("/internal/entitlements/release")
    public ResponseEntity<EntitlementOperationResponse> release(@Valid @RequestBody EntitlementOperationRequest request) {
        return ResponseEntity.ok(entitlementService.release(request));
    }

    @PostMapping("/internal/entitlements/refund")
    public ResponseEntity<EntitlementOperationResponse> refund(@Valid @RequestBody EntitlementOperationRequest request) {
        return ResponseEntity.ok(entitlementService.refund(request));
    }
}
