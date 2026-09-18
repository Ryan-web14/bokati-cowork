package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.CouponLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Branche le module coupon sur le moteur, sans que le moteur en sache davantage. */
@Component
@RequiredArgsConstructor
public class CouponLookupAdapter implements CouponLookup {

    private final CouponService couponService;

    @Override
    @Transactional(readOnly = true)
    public List<ResolvedCoupon> resolve(List<String> codes, String subscriberType, String subscriberCode, Instant at) {
        List<ResolvedCoupon> resolved = new ArrayList<>();
        if (codes == null) {
            return resolved;
        }
        for (String code : codes) {
            Coupon coupon = couponService.find(code).orElse(null);
            if (coupon == null) {
                // Meme message qu'un code refuse pour appartenance : dire « inconnu » plutot que
                // « pas a vous » evite de confirmer l'existence d'un code a qui le devine.
                resolved.add(new ResolvedCoupon(code, null, "Ce code est introuvable"));
                continue;
            }
            String reason = couponService.rejectionReason(coupon, subscriberType, subscriberCode, at);
            resolved.add(new ResolvedCoupon(coupon.getCode(), reason == null ? coupon.getPromotion() : null, reason));
        }
        return resolved;
    }
}
