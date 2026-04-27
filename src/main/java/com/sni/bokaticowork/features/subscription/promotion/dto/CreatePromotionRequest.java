package com.sni.bokaticowork.features.subscription.promotion.dto;

import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
public record CreatePromotionRequest(
        @NotBlank String name,
        String description,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.00") BigDecimal discountValue,
        @NotBlank String startsAt,
        String endsAt,
        Integer maxRedemptions,
        String metadataJson
) {
}
