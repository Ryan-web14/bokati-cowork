package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PlanEntitlementRequest(
        @NotBlank String entitlementCode,
        BigDecimal quantity,
        Boolean unlimited,
        Boolean rolloverAllowed,
        BigDecimal rolloverLimit,
        Integer validForDays,
        Integer priority,
        String restrictionsJson
) {
}
