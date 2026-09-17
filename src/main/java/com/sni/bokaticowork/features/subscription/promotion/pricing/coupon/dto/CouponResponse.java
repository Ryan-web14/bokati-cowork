package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;

import java.time.Instant;

/** Ce qu'un client voit d'un code dont il dispose. */
public record CouponResponse(
        String code,
        String promotionCode,
        String promotionName,
        String promotionDescription,
        CouponKind couponKind,
        CouponStatus status,
        Integer remainingUses,
        Instant validFrom,
        Instant validUntil,
        String batchCode
) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getCode(),
                coupon.getPromotion() == null ? null : coupon.getPromotion().getCode(),
                coupon.getPromotion() == null ? null : coupon.getPromotion().getName(),
                coupon.getPromotion() == null ? null : coupon.getPromotion().getDescription(),
                coupon.getCouponKind(),
                coupon.getStatus(),
                coupon.remainingUses(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getBatch() == null ? null : coupon.getBatch().getBatchCode());
    }
}
