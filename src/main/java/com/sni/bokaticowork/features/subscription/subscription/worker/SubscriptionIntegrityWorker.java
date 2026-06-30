package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionIntegrityWorker {

    private final SubscriptionService subscriptionService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.integrity-delay-ms:1800000}")
    public void repairActiveSubscriptionsWithoutGrants() {
        try {
            int repaired = subscriptionService.repairActiveSubscriptionsWithoutGrants();
            if (repaired > 0) {
                log.warn("Repaired {} active subscriptions without entitlement grants", repaired);
            }
        } catch (Exception ex) {
            log.error("SubscriptionIntegrityWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
