package com.sni.bokaticowork.features.subscription.repository.specification.criteria;

import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PlanSearchCriteria {
    private String code;
    private String name;
    private PlanType planType;
    private TargetAudience targetAudience;
    private PlanStatus status;
    private Boolean visible;
}
