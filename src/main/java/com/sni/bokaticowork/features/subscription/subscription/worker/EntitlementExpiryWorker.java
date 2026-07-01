package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntitlementExpiryWorker {

    private final EntitlementService entitlementService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.entitlement-expiry-delay-ms:600000}")
    public void expireGrants() {
        try {
            int expired = entitlementService.expireGrants();
            if (expired > 0) {
                log.info("Expired {} entitlement grants", expired);
            }
        } catch (Exception ex) {
            log.error("EntitlementExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
