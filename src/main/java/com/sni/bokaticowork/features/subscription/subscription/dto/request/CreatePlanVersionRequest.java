package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record CreatePlanVersionRequest(
        @NotBlank String name,
        String description,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String termsJson,
        @Valid List<PlanPriceRequest> prices,
        @Valid List<PlanBenefitRequest> benefits,
        @Valid List<PlanEntitlementRequest> entitlements
) {
}
