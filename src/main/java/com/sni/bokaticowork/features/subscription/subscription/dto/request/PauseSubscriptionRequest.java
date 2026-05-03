package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record PauseSubscriptionRequest(
        @Min(1) Integer days,
        LocalDate resumeDate,
        String reason,
        String changedBy
) {
}
