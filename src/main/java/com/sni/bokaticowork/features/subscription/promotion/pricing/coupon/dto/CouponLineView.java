package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;

import java.time.Instant;

/**
 * Un code d'un lot, tel que l'administration le suit. Ne lit que des colonnes du coupon : la ligne
 * se construit sans toucher a la promotion ni au lot, deja portes par la vue du lot.
 */
public record CouponLineView(
        String code,
        CouponStatus status,
        CouponKind couponKind,
        String assignedToType,
        String assignedToCode,
        Integer maxRedemptions,
        Integer redemptionCount,
        Integer reservedCount,
        Instant validFrom,
        Instant validUntil,
        Instant issuedAt,
        Instant revokedAt,
        String revokedReason
) {

    public static CouponLineView of(Coupon coupon) {
        return new CouponLineView(
                coupon.getCode(),
                coupon.getStatus(),
                coupon.getCouponKind(),
                coupon.getAssignedToType(),
                coupon.getAssignedToCode(),
                coupon.effectiveMaxRedemptions(),
                coupon.getRedemptionCount(),
                coupon.getReservedCount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getIssuedAt(),
                coupon.getRevokedAt(),
                coupon.getRevokedReason());
    }
}
