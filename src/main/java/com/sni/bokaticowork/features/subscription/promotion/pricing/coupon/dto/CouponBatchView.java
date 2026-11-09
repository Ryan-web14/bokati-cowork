package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponKind;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ce que l'administration voit d'un lot : sa definition, et ou en est la campagne.
 *
 * <p>Les compteurs sont lus sur les coupons au moment de la consultation, pas sur le lot : le
 * compteur stocke du lot n'est pas tenu a jour par la consommation, et un taux d'utilisation faux
 * est pire qu'absent.</p>
 */
public record CouponBatchView(
        String batchCode,
        String name,
        String description,
        String promotionCode,
        String promotionName,
        CouponKind couponKind,
        String codePrefix,
        Integer codeLength,
        Integer requestedQuantity,
        Integer generatedQuantity,
        Integer maxRedemptionsPerCoupon,
        Instant validFrom,
        Instant validUntil,
        String channel,
        Instant createdAt,
        String createdBy,
        long totalCoupons,
        Map<CouponStatus, Long> countsByStatus,
        long totalRedemptions,
        long totalReserved
) {

    /** Compteurs d'un lot, chaque statut present meme a zero pour que l'ecran n'ait rien a deviner. */
    public record Counts(Map<CouponStatus, Long> byStatus, long redemptions, long reserved) {

        public static Counts empty() {
            return new Counts(zeroFilled(), 0, 0);
        }

        public static Map<CouponStatus, Long> zeroFilled() {
            Map<CouponStatus, Long> map = new LinkedHashMap<>();
            for (CouponStatus status : CouponStatus.values()) {
                map.put(status, 0L);
            }
            return map;
        }

        public long total() {
            return byStatus.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    /** A appeler dans la transaction qui a lu le lot : la promotion y est lue. */
    public static CouponBatchView of(CouponBatch batch, Counts counts) {
        Counts safe = counts == null ? Counts.empty() : counts;
        return new CouponBatchView(
                batch.getBatchCode(),
                batch.getName(),
                batch.getDescription(),
                batch.getPromotion() == null ? null : batch.getPromotion().getCode(),
                batch.getPromotion() == null ? null : batch.getPromotion().getName(),
                batch.getCouponKind(),
                batch.getCodePrefix(),
                batch.getCodeLength(),
                batch.getRequestedQuantity(),
                batch.getGeneratedQuantity(),
                batch.getMaxRedemptionsPerCoupon(),
                batch.getValidFrom(),
                batch.getValidUntil(),
                batch.getChannel(),
                batch.getCreatedAt(),
                batch.getCreatedBy(),
                safe.total(),
                safe.byStatus(),
                safe.redemptions(),
                safe.reserved());
    }
}
