package com.sni.bokaticowork.features.subscription.change.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.change.dto.CreateSubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.dto.SubscriptionChangeResponse;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeStatus;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;
import com.sni.bokaticowork.features.subscription.change.service.SubscriptionChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions")
@RequiredArgsConstructor
public class SubscriptionChangeRequestController {

    private final SubscriptionChangeRequestService changeService;

    @PostMapping("/{subscriptionNumber}/changes")
    public ResponseEntity<SubscriptionChangeResponse> request(@PathVariable String subscriptionNumber,
                                                              @Valid @RequestBody CreateSubscriptionChangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(changeService.request(subscriptionNumber, request));
    }

    @PatchMapping("/changes/{changeNumber}/approve")
    public ResponseEntity<SubscriptionChangeResponse> approve(@PathVariable String changeNumber,
                                                              @RequestBody(required = false) Map<String, String> request) {
        return ResponseEntity.ok(changeService.approve(changeNumber, request == null ? null : request.get("approvedBy")));
    }

    @PatchMapping("/changes/{changeNumber}/apply")
    public ResponseEntity<SubscriptionChangeResponse> apply(@PathVariable String changeNumber) {
        return ResponseEntity.ok(changeService.apply(changeNumber));
    }

    @GetMapping("/changes")
    public ResponseEntity<PaginatedResponse<SubscriptionChangeResponse>> list(
            @RequestParam(required = false) String subscriptionNumber,
            @RequestParam(required = false) SubscriptionChangeType changeType,
            @RequestParam(required = false) SubscriptionChangeStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(changeService.list(subscriptionNumber, changeType, status, pageable));
    }
}
