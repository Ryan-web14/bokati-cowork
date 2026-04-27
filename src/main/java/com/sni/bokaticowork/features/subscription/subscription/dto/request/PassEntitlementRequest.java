package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;

public record PassEntitlementRequest(
        @NotBlank String entitlementCode,
        BigDecimal quantity,
        Boolean unlimited,
        Instant validFrom,
        Instant validUntil
) {
}
