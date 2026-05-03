package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record BookingWaitlistEntryResponse(
        Long id,
        String resourceCode,
        String resourceName,
        SubscriberType ownerType,
        String ownerCode,
        String contactName,
        String contactEmail,
        String contactPhone,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer quantity,
        BookingPaymentMode paymentMode,
        BookingWaitlistStatus status,
        Instant offeredAt,
        Instant expiresAt,
        Instant createdAt,
        List<BookingSuggestionResponse> alternatives
) {
}
