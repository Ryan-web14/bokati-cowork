package com.sni.bokaticowork.features.subscription.timeline.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.timeline.dto.CreateTimelineEventRequest;
import com.sni.bokaticowork.features.subscription.timeline.dto.SubscriptionTimelineEventResponse;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping(ApiPath.V1 + "/subscription-timeline")
@RequiredArgsConstructor
public class SubscriptionTimelineController {

    private final SubscriptionTimelineService timelineService;

    @PostMapping
    public ResponseEntity<SubscriptionTimelineEventResponse> create(@Valid @RequestBody CreateTimelineEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timelineService.create(request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<SubscriptionTimelineEventResponse>> list(
            @RequestParam(required = false) String subscriptionNumber,
            @RequestParam(required = false) SubscriberType ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) SubscriptionTimelineEventType eventType,
            @RequestParam(required = false) Instant occurredFrom,
            @RequestParam(required = false) Instant occurredTo,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(timelineService.list(subscriptionNumber, ownerType, ownerCode, eventType, occurredFrom, occurredTo, pageable));
    }
}
