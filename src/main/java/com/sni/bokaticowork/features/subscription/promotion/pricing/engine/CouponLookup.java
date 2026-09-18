package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;

import java.time.Instant;
import java.util.List;

/**
 * Ce que le moteur a besoin de savoir d'un code saisi, et rien de plus.
 *
 * <p>Le moteur ne connait ni la table des coupons, ni leurs retenues, ni leurs lots. Il demande si
 * un code est utilisable et quelle campagne il porte. Cette frontiere le garde testable sans le
 * module coupon, et empeche l'evaluation de consommer quoi que ce soit au passage.</p>
 */
public interface CouponLookup {

    /**
     * @param promotion campagne portee par le code, nulle si le code est refuse
     * @param rejectionReason motif du refus redige en francais, nul si le code est accepte
     */
    record ResolvedCoupon(String code, Promotion promotion, String rejectionReason) {

        public boolean accepted() {
            return rejectionReason == null && promotion != null;
        }
    }

    List<ResolvedCoupon> resolve(List<String> codes, String subscriberType, String subscriberCode, Instant at);
}
