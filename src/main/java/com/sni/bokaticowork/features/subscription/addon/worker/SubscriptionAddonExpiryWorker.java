package com.sni.bokaticowork.features.subscription.addon.worker;

import com.sni.bokaticowork.features.subscription.addon.service.SubscriptionAddonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionAddonExpiryWorker {

    private final SubscriptionAddonService addonService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.addon-expiry-delay-ms:900000}")
    public void expireEndedAddons() {
        try {
            int expired = addonService.expireEndedAddons();
            if (expired > 0) {
                log.info("Expired {} subscription add-ons", expired);
            }
        } catch (Exception ex) {
            log.error("SubscriptionAddonExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
