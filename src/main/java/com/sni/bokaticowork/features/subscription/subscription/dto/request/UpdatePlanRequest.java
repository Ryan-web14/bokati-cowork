package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;

public record UpdatePlanRequest(
        String name,
        String description,
        PlanType planType,
        TargetAudience targetAudience,
        Boolean visible,
        Integer sortOrder
) {
}
