package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;

import java.time.Instant;
import java.util.List;

public record PlanResponse(
        Long id,
        String code,
        String name,
        String description,
        PlanType planType,
        TargetAudience targetAudience,
        PlanStatus status,
        Boolean visible,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt,
        PlanVersionResponse activeVersion,
        List<PlanVersionResponse> versions
) {
}
