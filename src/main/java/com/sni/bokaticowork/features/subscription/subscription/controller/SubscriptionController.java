package com.sni.bokaticowork.features.subscription.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PauseSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillingScheduleResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionHistoryResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.SubscriptionSearchCriteria;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(request));
    }

    @GetMapping("/{subscriptionNumber}")
    public ResponseEntity<SubscriptionResponse> get(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(subscriptionService.get(subscriptionNumber));
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> current(@RequestParam SubscriberType subscriberType,
                                                        @RequestParam String subscriberCode) {
        return ResponseEntity.ok(subscriptionService.current(subscriberType, subscriberCode));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<SubscriptionResponse>> list(
            @RequestParam(required = false) SubscriberType subscriberType,
            @RequestParam(required = false) String subscriberCode,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) LocalDate nextBillingBefore,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.list(
                SubscriptionSearchCriteria.builder()
                        .subscriberType(subscriberType)
                        .subscriberCode(subscriberCode)
                        .planCode(planCode)
                        .status(status)
                        .nextBillingBefore(nextBillingBefore)
                        .build(),
                pageable
        ));
    }

    @PatchMapping("/{subscriptionNumber}/activate")
    public ResponseEntity<SubscriptionResponse> activate(@PathVariable String subscriptionNumber,
                                                         @RequestBody(required = false) SubscriptionStatusChangeRequest request) {
        return ResponseEntity.ok(subscriptionService.activate(subscriptionNumber, request));
    }

    @PatchMapping("/{subscriptionNumber}/suspend")
    public ResponseEntity<SubscriptionResponse> suspend(@PathVariable String subscriptionNumber,
                                                        @RequestBody(required = false) SubscriptionStatusChangeRequest request) {
        return ResponseEntity.ok(subscriptionService.suspend(subscriptionNumber, request));
    }

    @PatchMapping("/{subscriptionNumber}/pause")
    public ResponseEntity<SubscriptionResponse> pause(@PathVariable String subscriptionNumber,
                                                      @Valid @RequestBody PauseSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.pause(subscriptionNumber, request));
    }

    @PatchMapping("/{subscriptionNumber}/resume")
    public ResponseEntity<SubscriptionResponse> resume(@PathVariable String subscriptionNumber,
                                                       @RequestBody(required = false) SubscriptionStatusChangeRequest request) {
        return ResponseEntity.ok(subscriptionService.resume(subscriptionNumber, request));
    }

    @PatchMapping("/{subscriptionNumber}/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(@PathVariable String subscriptionNumber,
                                                       @RequestBody(required = false) SubscriptionStatusChangeRequest request) {
        return ResponseEntity.ok(subscriptionService.cancel(subscriptionNumber, request));
    }

    @PatchMapping("/{subscriptionNumber}/renew")
    public ResponseEntity<SubscriptionResponse> renew(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(subscriptionService.renew(subscriptionNumber));
    }

    @GetMapping("/{subscriptionNumber}/entitlements")
    public ResponseEntity<List<EntitlementGrantResponse>> entitlements(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(subscriptionService.listEntitlements(subscriptionNumber));
    }

    @GetMapping("/{subscriptionNumber}/history")
    public ResponseEntity<List<SubscriptionHistoryResponse>> history(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(subscriptionService.history(subscriptionNumber));
    }

    @GetMapping("/{subscriptionNumber}/billing-schedule")
    public ResponseEntity<BillingScheduleResponse> billingSchedule(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(subscriptionService.billingSchedule(subscriptionNumber));
    }
}
