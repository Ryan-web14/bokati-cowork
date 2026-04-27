package com.sni.bokaticowork.features.subscription.seat.dto;

import com.sni.bokaticowork.features.subscription.seat.enums.SeatRole;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;

import java.time.Instant;

public record SubscriptionSeatResponse(
        Long id,
        String subscriptionNumber,
        String memberCode,
        String memberName,
        SeatRole role,
        SeatStatus status,
        Instant invitedAt,
        Instant activatedAt,
        Instant removedAt
) {
}
