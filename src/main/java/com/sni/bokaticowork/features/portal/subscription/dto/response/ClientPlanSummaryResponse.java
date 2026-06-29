package com.sni.bokaticowork.features.portal.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ClientPlanSummaryResponse(
        String code,
        String name,
        String description,
        PlanType planType,
        TargetAudience targetAudience,
        Integer sortOrder,
        BigDecimal startingPrice,
        String currency,
        List<HighlightedBenefit> highlightedBenefits
) {

    @Builder
    public record HighlightedBenefit(
            String title,
            String description,
            String icon
    ) {
    }
}
