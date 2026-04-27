package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;

import java.time.LocalDate;
import java.util.List;

public record PlanVersionResponse(
        String id,
        Integer versionNumber,
        String name,
        String description,
        PlanStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String termsJson,
        List<PlanPriceResponse> prices,
        List<PlanBenefitResponse> benefits,
        List<PlanEntitlementResponse> entitlements
) {
}
