package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePlanRequest(
        @NotBlank String name,
        String description,
        @NotNull PlanType planType,
        @NotNull TargetAudience targetAudience,
        Boolean visible,
        Integer sortOrder
) {
}
