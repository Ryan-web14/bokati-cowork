package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingStatus;

import java.time.LocalDateTime;

public record BookingCheckInScanResponse(
        String bookingNumber,
        String resourceName,
        String contactName,
        BookingStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
