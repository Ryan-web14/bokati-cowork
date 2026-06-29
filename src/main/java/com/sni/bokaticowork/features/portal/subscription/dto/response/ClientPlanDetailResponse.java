package com.sni.bokaticowork.features.portal.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanBenefitResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanEntitlementResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanPriceResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import lombok.Builder;

import java.util.List;

@Builder
public record ClientPlanDetailResponse(
        String code,
        String name,
        String description,
        PlanType planType,
        TargetAudience targetAudience,
        Integer sortOrder,
        Integer requiredKycLevel,
        String termsJson,
        List<PlanPriceResponse> prices,
        List<PlanBenefitResponse> benefits,
        List<PlanEntitlementResponse> entitlements
) {
}
