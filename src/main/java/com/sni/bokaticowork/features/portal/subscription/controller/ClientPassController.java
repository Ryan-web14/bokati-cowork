package com.sni.bokaticowork.features.portal.subscription.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.subscription.service.ClientSubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClientPassController {

    private final ClientContextService clientContextService;
    private final ClientSubscriptionService clientSubscriptionService;

    // ── Passes ────────────────────────────────────────────────────────────────

    @GetMapping(ApiPath.V1 + "/client/passes")
    public ResponseEntity<PaginatedResponse<PassResponse>> listPasses(
            @RequestParam(required = false) PassType passType,
            @RequestParam(required = false) PassStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.listPasses(member, passType, status, pageable));
    }

    @GetMapping(ApiPath.V1 + "/client/passes/{passNumber}")
    public ResponseEntity<PassResponse> getPass(@PathVariable String passNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.getPass(member, passNumber));
    }

    // ── Entitlement balances ───────────────────────────────────────────────────

    @GetMapping(ApiPath.V1 + "/client/entitlements/balances")
    public ResponseEntity<List<EntitlementGrantResponse>> getEntitlementBalances() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSubscriptionService.getEntitlementBalances(member));
    }
}
