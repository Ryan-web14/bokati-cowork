package com.sni.bokaticowork.features.booking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BookingChangeResourceRequest(
        @NotBlank String newResourceCode,
        String actor,
        String note,
        Boolean sendEmail
) {}
