package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        String bookingNumber,
        String resourceCode,
        String resourceName,
        String resourceTypeCode,
        String resourceGroupCode,
        SubscriberType ownerType,
        String ownerCode,
        String contactName,
        String contactEmail,
        String contactPhone,
        BookingStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer durationMinutes,
        Integer quantity,
        ResourceBookingUnit bookingUnit,
        BookingPaymentMode paymentMode,
        String subscriptionNumber,
        String passNumber,
        String entitlementCode,
        BigDecimal unitPrice,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        String currency,
        String notes,
        String metadataJson,
        Instant confirmedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        List<BookingLineResponse> lines,
        List<BookingParticipantResponse> participants
) {
}
