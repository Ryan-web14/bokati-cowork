package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateCouponBatchRequest(
        @NotBlank String promotionCode,
        @NotBlank String name,
        @Min(1) @Max(50000) int quantity,
        @NotNull CouponKind couponKind,
        String codePrefix,
        @Min(6) @Max(32) Integer codeLength,
        Integer maxRedemptionsPerCoupon,
        Instant validFrom,
        Instant validUntil,
        String channel
) {

    public int codeLengthOrDefault() {
        return codeLength == null ? 10 : codeLength;
    }
}
