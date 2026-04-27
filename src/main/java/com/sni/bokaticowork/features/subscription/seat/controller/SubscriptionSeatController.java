package com.sni.bokaticowork.features.subscription.seat.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.seat.dto.CreateSubscriptionSeatRequest;
import com.sni.bokaticowork.features.subscription.seat.dto.SubscriptionSeatResponse;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;
import com.sni.bokaticowork.features.subscription.seat.service.SubscriptionSeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions")
@RequiredArgsConstructor
public class SubscriptionSeatController {

    private final SubscriptionSeatService seatService;

    @PostMapping("/{subscriptionNumber}/seats")
    public ResponseEntity<SubscriptionSeatResponse> add(@PathVariable String subscriptionNumber,
                                                        @Valid @RequestBody CreateSubscriptionSeatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.add(subscriptionNumber, request));
    }

    @DeleteMapping("/{subscriptionNumber}/seats/{memberCode}")
    public ResponseEntity<SubscriptionSeatResponse> remove(@PathVariable String subscriptionNumber, @PathVariable String memberCode) {
        return ResponseEntity.ok(seatService.remove(subscriptionNumber, memberCode));
    }

    @GetMapping("/seats")
    public ResponseEntity<PaginatedResponse<SubscriptionSeatResponse>> list(
            @RequestParam(required = false) String subscriptionNumber,
            @RequestParam(required = false) String memberCode,
            @RequestParam(required = false) SeatStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(seatService.list(subscriptionNumber, memberCode, status, pageable));
    }
}
