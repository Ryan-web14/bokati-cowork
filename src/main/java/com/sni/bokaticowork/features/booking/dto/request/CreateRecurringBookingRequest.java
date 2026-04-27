package com.sni.bokaticowork.features.booking.dto.request;

import com.sni.bokaticowork.features.booking.enums.BookingRecurrenceFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateRecurringBookingRequest(
        @Valid @NotNull CreateBookingRequest booking,
        @NotNull BookingRecurrenceFrequency frequency,
        @Min(1) Integer intervalValue,
        @Min(2) @Max(60) Integer occurrences
) {
}
