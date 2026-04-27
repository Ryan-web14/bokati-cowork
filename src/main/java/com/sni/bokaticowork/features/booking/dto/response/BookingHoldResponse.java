package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingHoldStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.time.Instant;
import java.time.LocalDateTime;

public record BookingHoldResponse(
        String holdNumber,
        String resourceCode,
        SubscriberType ownerType,
        String ownerCode,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer quantity,
        BookingHoldStatus status,
        Instant expiresAt
) {
}
