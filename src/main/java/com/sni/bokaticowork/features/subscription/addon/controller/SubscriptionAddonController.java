package com.sni.bokaticowork.features.subscription.addon.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.addon.dto.CreateSubscriptionAddonRequest;
import com.sni.bokaticowork.features.subscription.addon.dto.SubscriptionAddonResponse;
import com.sni.bokaticowork.features.subscription.addon.enums.SubscriptionAddonStatus;
import com.sni.bokaticowork.features.subscription.addon.service.SubscriptionAddonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions")
@RequiredArgsConstructor
public class SubscriptionAddonController {

    private final SubscriptionAddonService addonService;

    @PostMapping("/{subscriptionNumber}/addons")
    public ResponseEntity<SubscriptionAddonResponse> add(@PathVariable String subscriptionNumber,
                                                         @Valid @RequestBody CreateSubscriptionAddonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addonService.add(subscriptionNumber, request));
    }

    @PatchMapping("/addons/{addonId}/cancel")
    public ResponseEntity<SubscriptionAddonResponse> cancel(@PathVariable Long addonId) {
        return ResponseEntity.ok(addonService.cancel(addonId));
    }

    @GetMapping("/addons")
    public ResponseEntity<PaginatedResponse<SubscriptionAddonResponse>> list(
            @RequestParam(required = false) String subscriptionNumber,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) SubscriptionAddonStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(addonService.list(subscriptionNumber, planCode, status, pageable));
    }
}
