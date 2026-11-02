package com.sni.bokaticowork.features.portal.company.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.company.sharing.dto.SharingDtos;
import com.sni.bokaticowork.features.company.sharing.service.CompanySharingService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * L'abonnement partage dans le portail · le gerant attribue et ajuste les places, chaque
 * collaborateur voit ce qu'il peut prendre.
 *
 * <p>Le gerant regle aussi la repartition de chaque droit et son nombre de places, sous le
 * plafond vendu que fixe l'administration.</p>
 */
@RestController
@RequiredArgsConstructor
public class ClientCompanySharingController {

    private final ClientContextService clientContextService;
    private final CompanyMembershipService membershipService;
    private final CompanySharingService service;

    /** Mes places sur les abonnements de mes organisations · ma part, mon plafond, le pot restant. */
    @GetMapping(ApiPath.V1 + "/client/me/entitlements")
    public ResponseEntity<List<SharingDtos.MySeat>> mySeats() {
        return ResponseEntity.ok(service.mySeats(clientContextService.getAuthenticatedMember()));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/subscriptions")
    public ResponseEntity<List<SharingDtos.SubscriptionSummary>> list(@PathVariable String customerId) {
        return ResponseEntity.ok(service.subscriptions(owned(customerId)));
    }

    /** La consommation par collaborateur, sur tous les abonnements ouverts. */
    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/usage")
    public ResponseEntity<List<SharingDtos.SharingView>> usage(@PathVariable String customerId) {
        return ResponseEntity.ok(service.usage(owned(customerId)));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/subscriptions/{subscriptionNumber}/sharing")
    public ResponseEntity<SharingDtos.SharingView> view(@PathVariable String customerId, @PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(service.view(owned(customerId), subscriptionNumber));
    }

    @PutMapping(ApiPath.V1 + "/client/companies/{customerId}/subscriptions/{subscriptionNumber}/sharing/{entitlementCode}")
    @Audited(module = "COMPANY", action = "SHARING_RULE_PORTAL", ressource = "subscription_entitlement_sharing")
    public ResponseEntity<SharingDtos.SharingView> setRule(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                           @PathVariable String entitlementCode,
                                                           @Valid @RequestBody SharingDtos.RuleRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.setRule(membershipService.ownedCompany(owner, customerId), subscriptionNumber,
                entitlementCode, request, owner.getMemberId()));
    }

    @PatchMapping(ApiPath.V1 + "/client/companies/{customerId}/subscriptions/{subscriptionNumber}/max-seats")
    @Audited(module = "COMPANY", action = "SHARING_MAX_SEATS_PORTAL", ressource = "subscription")
    public ResponseEntity<SharingDtos.SharingView> setMaxSeats(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                               @Valid @RequestBody SharingDtos.MaxSeatsRequest request) {
        return ResponseEntity.ok(service.setMaxSeatsByOwner(owned(customerId), subscriptionNumber, request.maxSeats()));
    }

    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/subscriptions/{subscriptionNumber}/seats")
    @Audited(module = "COMPANY", action = "SEAT_ASSIGN_PORTAL", ressource = "subscription_seat")
    public ResponseEntity<SharingDtos.SharingView> assign(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                          @Valid @RequestBody SharingDtos.AssignSeatRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.assignSeat(membershipService.ownedCompany(owner, customerId), subscriptionNumber,
                request, owner.getMemberId()));
    }

    @PatchMapping(ApiPath.V1 + "/client/companies/{customerId}/subscriptions/{subscriptionNumber}/seats/{seatId}")
    @Audited(module = "COMPANY", action = "SEAT_UPDATE_PORTAL", ressource = "subscription_seat")
    public ResponseEntity<SharingDtos.SharingView> update(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                          @PathVariable Long seatId,
                                                          @Valid @RequestBody SharingDtos.UpdateSeatRequest request) {
        return ResponseEntity.ok(service.updateSeat(owned(customerId), subscriptionNumber, seatId, request));
    }

    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/subscriptions/{subscriptionNumber}/seats/{seatId}/release")
    @Audited(module = "COMPANY", action = "SEAT_RELEASE_PORTAL", ressource = "subscription_seat")
    public ResponseEntity<SharingDtos.ReleaseResult> release(@PathVariable String customerId, @PathVariable String subscriptionNumber,
                                                             @PathVariable Long seatId,
                                                             @Valid @RequestBody(required = false) SharingDtos.ReleaseSeatRequest request) {
        return ResponseEntity.ok(service.releaseSeat(owned(customerId), subscriptionNumber, seatId,
                request == null ? null : request.reason()));
    }

    private Customer owned(String customerId) {
        return membershipService.ownedCompany(clientContextService.getAuthenticatedMember(), customerId);
    }
}
