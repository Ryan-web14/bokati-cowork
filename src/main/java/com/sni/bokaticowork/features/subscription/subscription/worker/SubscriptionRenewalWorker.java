package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalWorker {

    private final SubscriptionService subscriptionService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.renewal-delay-ms:900000}")
    public void renewDueSubscriptions() {
        int renewed = subscriptionService.renewDueSubscriptions();
        if (renewed > 0) {
            log.info("Renewed {} due subscriptions", renewed);
        }
    }
}
