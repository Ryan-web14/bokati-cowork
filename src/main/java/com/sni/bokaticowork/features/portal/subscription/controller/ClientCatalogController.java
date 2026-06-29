package com.sni.bokaticowork.features.portal.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.subscription.dto.request.ClientPurchasePassRequest;
import com.sni.bokaticowork.features.portal.subscription.dto.request.ClientSubscribeRequest;
import com.sni.bokaticowork.features.portal.subscription.dto.response.ClientPlanDetailResponse;
import com.sni.bokaticowork.features.portal.subscription.dto.response.ClientPlanSummaryResponse;
import com.sni.bokaticowork.features.portal.subscription.service.ClientCatalogService;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/client/catalog")
@RequiredArgsConstructor
public class ClientCatalogController {

    private final ClientContextService clientContextService;
    private final ClientCatalogService clientCatalogService;

    @GetMapping("/plans")
    public ResponseEntity<PaginatedResponse<ClientPlanSummaryResponse>> listAvailablePlans(
            @RequestParam(required = false) PlanType planType,
            @PageableDefault(size = 20, sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(clientCatalogService.listAvailablePlans(planType, pageable));
    }

    @GetMapping("/plans/{planCode}")
    public ResponseEntity<ClientPlanDetailResponse> getPlanDetail(@PathVariable String planCode) {
        return ResponseEntity.ok(clientCatalogService.getPlanDetail(planCode));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody ClientSubscribeRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientCatalogService.subscribe(member, request));
    }

    @PostMapping("/passes/purchase")
    public ResponseEntity<PassResponse> purchasePass(
            @Valid @RequestBody ClientPurchasePassRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientCatalogService.purchasePass(member, request));
    }
}
