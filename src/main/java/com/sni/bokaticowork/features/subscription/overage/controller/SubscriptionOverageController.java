package com.sni.bokaticowork.features.subscription.overage.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.overage.dto.CreateOveragePolicyRequest;
import com.sni.bokaticowork.features.subscription.overage.dto.OverageChargeResponse;
import com.sni.bokaticowork.features.subscription.overage.dto.OveragePolicyResponse;
import com.sni.bokaticowork.features.subscription.overage.service.SubscriptionOverageService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/subscription-overage")
@RequiredArgsConstructor
public class SubscriptionOverageController {

    private final SubscriptionOverageService overageService;

    @PostMapping("/policies")
    public ResponseEntity<OveragePolicyResponse> createPolicy(@Valid @RequestBody CreateOveragePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(overageService.createPolicy(request));
    }

    @GetMapping("/policies")
    public ResponseEntity<List<OveragePolicyResponse>> listPolicies(@RequestParam(required = false) Long planVersionId,
                                                                    @RequestParam(required = false) String entitlementCode) {
        return ResponseEntity.ok(overageService.listPolicies(planVersionId, entitlementCode));
    }

    @GetMapping("/charges")
    public ResponseEntity<List<OverageChargeResponse>> listCharges(@RequestParam(required = false) String subscriptionNumber,
                                                                   @RequestParam(required = false) SubscriberType ownerType,
                                                                   @RequestParam(required = false) String ownerCode,
                                                                   @RequestParam(required = false) String entitlementCode) {
        return ResponseEntity.ok(overageService.listCharges(subscriptionNumber, ownerType, ownerCode, entitlementCode));
    }
}
