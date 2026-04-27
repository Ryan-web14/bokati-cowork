package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.BenefitCategory;

public record PlanBenefitResponse(
        Long id,
        String title,
        String description,
        String icon,
        BenefitCategory category,
        Integer displayOrder,
        Boolean highlighted,
        Boolean included,
        String metadataJson
) {
}
