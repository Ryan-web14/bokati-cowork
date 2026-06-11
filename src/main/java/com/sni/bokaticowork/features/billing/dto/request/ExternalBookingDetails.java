package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalBookingDetails(
        @NotBlank String resourceName,
        String resourceDescription,
        @NotNull LocalDateTime checkInAt,
        @NotNull LocalDateTime checkOutAt,
        @NotNull ResourceBookingUnit bookingUnit,
        @NotNull BigDecimal quantity,
        @NotNull BigDecimal unitPrice,
        String externalReference,
        String notes
) {
}
