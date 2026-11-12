package com.sni.bokaticowork.features.subscription.promotion.worker;

import com.sni.bokaticowork.features.subscription.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionExpiryWorker implements SchedulingConfigurer {

    private static final String PROMOTION_EXPIRY_DELAY_MS_KEY = "bokati.subscription.workers.promotion-expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<PromotionExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<PromotionExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expireEndedPromotions(), triggers.fixedDelay(PROMOTION_EXPIRY_DELAY_MS_KEY, 900000L));
    }

    private final PromotionService promotionService;

    public void expireEndedPromotions() {
        try {
            int expired = promotionService.expireEndedPromotions();
            if (expired > 0) {
                log.info("Expired {} promotions", expired);
            }
        } catch (Exception ex) {
            log.error("PromotionExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
