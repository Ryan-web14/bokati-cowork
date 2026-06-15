package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PassEntitlementRequest(
        @NotBlank String entitlementCode,
        BigDecimal quantity,
        Boolean unlimited,
        String validFrom,
        String validUntil
) {
}
