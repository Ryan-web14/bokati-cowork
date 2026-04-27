package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import java.math.BigDecimal;

public record PlanEntitlementResponse(
        Long id,
        String entitlementCode,
        String entitlementName,
        BigDecimal quantity,
        Boolean unlimited,
        Boolean rolloverAllowed,
        BigDecimal rolloverLimit,
        Integer validForDays,
        Integer priority,
        String restrictionsJson
) {
}
