package com.sni.bokaticowork.features.booking.dto.response;

import com.sni.bokaticowork.features.booking.enums.BookingLineType;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;

import java.math.BigDecimal;

public record BookingLineResponse(
        Long id,
        BookingLineType lineType,
        String description,
        BigDecimal quantity,
        ResourceBookingUnit unit,
        BigDecimal unitPrice,
        BigDecimal amount,
        String currency,
        String entitlementCode,
        String billableNumber
) {
}
