package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.BenefitCategory;
import jakarta.validation.constraints.NotBlank;

public record PlanBenefitRequest(
        @NotBlank String title,
        String description,
        String icon,
        BenefitCategory category,
        Integer displayOrder,
        Boolean highlighted,
        Boolean included,
        String metadataJson
) {
}
