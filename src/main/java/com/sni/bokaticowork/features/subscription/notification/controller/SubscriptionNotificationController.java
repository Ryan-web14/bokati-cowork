package com.sni.bokaticowork.features.subscription.notification.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.notification.dto.CreateSubscriptionNotificationRequest;
import com.sni.bokaticowork.features.subscription.notification.dto.SubscriptionNotificationResponse;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationStatus;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.notification.service.SubscriptionNotificationService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.V1 + "/subscription-notifications")
@RequiredArgsConstructor
public class SubscriptionNotificationController {

    private final SubscriptionNotificationService notificationService;

    @PostMapping
    public ResponseEntity<SubscriptionNotificationResponse> queue(@Valid @RequestBody CreateSubscriptionNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.queue(request));
    }

    @PatchMapping("/{notificationNumber}/cancel")
    public ResponseEntity<Integer> cancel(@PathVariable String notificationNumber) {
        return ResponseEntity.ok(notificationService.cancel(notificationNumber));
    }

    @PostMapping("/dispatch-due")
    public ResponseEntity<Integer> dispatchDue(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(notificationService.dispatchDue(limit));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<SubscriptionNotificationResponse>> list(
            @RequestParam(required = false) String subscriptionNumber,
            @RequestParam(required = false) SubscriberType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) SubscriptionNotificationType notificationType,
            @RequestParam(required = false) SubscriptionNotificationStatus status,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.list(subscriptionNumber, ownerType, ownerCode, notificationType, status, pageable));
    }
}
