package com.sni.bokaticowork.features.subscription.promotion.dto;

import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import com.sni.bokaticowork.features.subscription.promotion.enums.PromotionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionResponse(
        String code,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        Instant startsAt,
        Instant endsAt,
        Integer maxRedemptions,
        Integer redemptionCount,
        PromotionStatus status,
        String metadataJson
) {
}
