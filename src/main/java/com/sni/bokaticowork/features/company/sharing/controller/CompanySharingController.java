package com.sni.bokaticowork.features.company.sharing.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.sharing.dto.SharingDtos;
import com.sni.bokaticowork.features.company.sharing.service.CompanySharingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * L'abonnement d'une organisation partage entre ses collaborateurs · cote administration.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/companies/{customerId}/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class CompanySharingController {

    private final CompanySharingService service;

    @GetMapping
    public ResponseEntity<List<SharingDtos.SubscriptionSummary>> list(@PathVariable String customerId) {
        return ResponseEntity.ok(service.subscriptions(service.company(customerId)));
    }

    @GetMapping("/{subscriptionNumber}/sharing")
    public ResponseEntity<SharingDtos.SharingView> view(@PathVariable String customerId, @PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(service.view(service.company(customerId), subscriptionNumber));
    }

    @PutMapping("/{subscriptionNumber}/sharing/{entitlementCode}")
    @Audited(module = "COMPANY", action = "SHARING_RULE", ressource = "subscription_entitlement_sharing")
    public ResponseEntity<SharingDtos.SharingView> setRule(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                           @PathVariable String entitlementCode,
                                                           @Valid @RequestBody SharingDtos.RuleRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(service.setRule(service.company(customerId), subscriptionNumber, entitlementCode, request,
                actor(authentication)));
    }

    @PatchMapping("/{subscriptionNumber}/max-seats")
    @Audited(module = "COMPANY", action = "SHARING_MAX_SEATS", ressource = "subscription")
    public ResponseEntity<SharingDtos.SharingView> setMaxSeats(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                               @Valid @RequestBody SharingDtos.MaxSeatsRequest request) {
        return ResponseEntity.ok(service.setMaxSeats(service.company(customerId), subscriptionNumber, request.maxSeats()));
    }

    @PostMapping("/{subscriptionNumber}/seats")
    @Audited(module = "COMPANY", action = "SEAT_ASSIGN", ressource = "subscription_seat")
    public ResponseEntity<SharingDtos.SharingView> assign(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                          @Valid @RequestBody SharingDtos.AssignSeatRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(service.assignSeat(service.company(customerId), subscriptionNumber, request, actor(authentication)));
    }

    @PatchMapping("/{subscriptionNumber}/seats/{seatId}")
    @Audited(module = "COMPANY", action = "SEAT_UPDATE", ressource = "subscription_seat")
    public ResponseEntity<SharingDtos.SharingView> update(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                          @PathVariable Long seatId,
                                                          @Valid @RequestBody SharingDtos.UpdateSeatRequest request) {
        return ResponseEntity.ok(service.updateSeat(service.company(customerId), subscriptionNumber, seatId, request));
    }

    @PostMapping("/{subscriptionNumber}/seats/{seatId}/release")
    @Audited(module = "COMPANY", action = "SEAT_RELEASE", ressource = "subscription_seat")
    public ResponseEntity<SharingDtos.ReleaseResult> release(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                             @PathVariable Long seatId,
                                                             @Valid @RequestBody(required = false) SharingDtos.ReleaseSeatRequest request) {
        return ResponseEntity.ok(service.releaseSeat(service.company(customerId), subscriptionNumber, seatId,
                request == null ? null : request.reason()));
    }

    private static String actor(Authentication authentication) {
        return authentication == null ? "SYSTEM" : authentication.getName();
    }
}
