package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.worker;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

/**
 * Rend les codes retenus par des paniers abandonnes.
 *
 * <p>Sans ce traitement, un panier ferme sans payer immobiliserait son code jusqu'a la fin de la
 * campagne. Sur un code a usage unique, cela revient a le perdre.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReservationExpiryWorker implements SchedulingConfigurer {

    private static final String EXPIRY_WORKER_DELAY_MS_KEY = "bokati.promotion.coupon.expiry-worker-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<CouponReservationExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<CouponReservationExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().releaseExpiredReservations(), triggers.fixedDelay(EXPIRY_WORKER_DELAY_MS_KEY, 120000L));
    }

    private final CouponService couponService;

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
