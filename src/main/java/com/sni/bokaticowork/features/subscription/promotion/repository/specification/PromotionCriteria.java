package com.sni.bokaticowork.features.subscription.promotion.repository.specification;

import com.sni.bokaticowork.features.subscription.promotion.enums.PromotionStatus;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PromotionCriteria {
    private String code;
    private String name;
    private PromotionStatus status;
}
