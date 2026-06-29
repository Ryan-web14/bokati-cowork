package com.sni.bokaticowork.features.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BookingRescheduleRequest(
        @NotNull LocalDateTime newStartedAt,
        @NotNull LocalDateTime newEndedAt,
        String actor,
        String note,
        Boolean sendEmail
) {}
