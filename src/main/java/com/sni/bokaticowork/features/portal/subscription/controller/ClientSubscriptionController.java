package com.sni.bokaticowork.features.portal.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.portal.billing.dto.request.ClientInitiateMobileMoneyPaymentRequest;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.subscription.dto.request.ClientCancelSubscriptionRequest;
import com.sni.bokaticowork.features.portal.subscription.service.ClientSubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillingScheduleResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionHistoryResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/client/subscriptions")
@RequiredArgsConstructor
public class ClientSubscriptionController {

    private final ClientContextService clientContextService;
    private final ClientSubscriptionService clientSubscriptionService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<SubscriptionResponse>> listSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.listSubscriptions(member, status, pageable));
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.getCurrentSubscription(member));
    }

    @GetMapping("/{subscriptionNumber}")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @PathVariable String subscriptionNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.getSubscription(member, subscriptionNumber));
    }

    @DeleteMapping("/{subscriptionNumber}")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @PathVariable String subscriptionNumber,
            @Valid @RequestBody(required = false) ClientCancelSubscriptionRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.cancelSubscription(member, subscriptionNumber, request));
    }

    @GetMapping("/{subscriptionNumber}/entitlements")
    public ResponseEntity<List<EntitlementGrantResponse>> getSubscriptionEntitlements(
            @PathVariable String subscriptionNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.getSubscriptionEntitlements(member, subscriptionNumber));
    }

    @GetMapping("/{subscriptionNumber}/history")
    public ResponseEntity<List<SubscriptionHistoryResponse>> getSubscriptionHistory(
            @PathVariable String subscriptionNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.getSubscriptionHistory(member, subscriptionNumber));
    }

    @GetMapping("/{subscriptionNumber}/billing")
    public ResponseEntity<BillingScheduleResponse> getBillingSchedule(
            @PathVariable String subscriptionNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.getBillingSchedule(member, subscriptionNumber));
    }

    @PostMapping("/{subscriptionNumber}/pay/mobile-money")
    public ResponseEntity<MobileMoneyDepositResponse> payWithMobileMoney(
            @PathVariable String subscriptionNumber,
            @Valid @RequestBody ClientInitiateMobileMoneyPaymentRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.paySubscriptionWithMobileMoney(
                member, subscriptionNumber, request.getPhoneNumber(), request.getCorrespondent()));
    }

    @PostMapping("/{subscriptionNumber}/pay/wallet")
    public ResponseEntity<PaymentTransactionResponse> payWithWallet(
            @PathVariable String subscriptionNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.paySubscriptionWithWallet(member, subscriptionNumber));
    }
}
