package com.sni.bokaticowork.features.subscription.rollover.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.rollover.dto.RolloverRecordResponse;
import com.sni.bokaticowork.features.subscription.rollover.service.SubscriptionRolloverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/subscription-rollovers")
@RequiredArgsConstructor
public class SubscriptionRolloverController {

    private final SubscriptionRolloverService rolloverService;

    @PostMapping("/apply-due")
    public ResponseEntity<Integer> applyDue() {
        return ResponseEntity.ok(rolloverService.applyDueRollovers());
    }

    @GetMapping
    public ResponseEntity<List<RolloverRecordResponse>> list(@RequestParam(required = false) String subscriptionNumber,
                                                             @RequestParam(required = false) String entitlementCode) {
        return ResponseEntity.ok(rolloverService.list(subscriptionNumber, entitlementCode));
    }
}
