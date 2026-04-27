package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingAvailabilityResponse(
        String resourceCode,
        String resourceName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer durationMinutes,
        Integer quantity,
        boolean available,
        Integer remainingCapacity,
        ResourceBookingUnit bookingUnit,
        BigDecimal unitPrice,
        BigDecimal estimatedAmount,
        String currency,
        String message
) {
}
