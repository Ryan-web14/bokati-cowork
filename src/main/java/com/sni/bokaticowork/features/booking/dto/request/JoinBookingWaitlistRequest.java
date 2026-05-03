package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record JoinBookingWaitlistRequest(
        @NotBlank String resourceCode,
        @Valid BookingIdentityLookup identityLookup,
        @Future @NotNull LocalDateTime startedAt,
        @Future @NotNull LocalDateTime endedAt,
        @Positive Integer quantity,
        @NotNull BookingPaymentMode paymentMode
) {
}
