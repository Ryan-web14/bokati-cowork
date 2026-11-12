package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.dto;

/**
 * Bilan d'une revocation de lot. Les codes ecartes sont comptes a part : un lot dont la moitie a
 * deja servi ne doit pas laisser croire que tout a ete annule.
 */
public record CouponBatchRevocation(
        String batchCode,
        int total,
        int revoked,
        int alreadyRevoked,
        int exhausted,
        int expired
) {
}
