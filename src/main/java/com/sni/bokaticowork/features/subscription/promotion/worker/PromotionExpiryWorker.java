package com.sni.bokaticowork.features.subscription.promotion.worker;

import com.sni.bokaticowork.features.subscription.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionExpiryWorker {

    private final PromotionService promotionService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.promotion-expiry-delay-ms:900000}")
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
