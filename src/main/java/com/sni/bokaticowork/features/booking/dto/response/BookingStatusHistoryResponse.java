package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingStatus;

import java.time.Instant;

public record BookingStatusHistoryResponse(
        BookingStatus fromStatus,
        BookingStatus toStatus,
        String changedBy,
        String reason,
        Instant changedAt
) {
}
