package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.worker;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rend les codes retenus par des paniers abandonnes.
 *
 * <p>Sans ce traitement, un panier ferme sans payer immobiliserait son code jusqu'a la fin de la
 * campagne. Sur un code a usage unique, cela revient a le perdre.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReservationExpiryWorker {

    private final CouponService couponService;

    @Scheduled(fixedDelayString = "${bokati.promotion.coupon.expiry-worker-delay-ms:120000}")
    public void releaseExpiredReservations() {
        try {
            int released = couponService.releaseExpired();
            if (released > 0) {
                log.info("Coupons : {} retenue(s) echue(s) rendue(s)", released);
            }
        } catch (Exception ex) {
            log.error("CouponReservationExpiryWorker a echoue : {}", ex.getMessage(), ex);
        }
    }
}
