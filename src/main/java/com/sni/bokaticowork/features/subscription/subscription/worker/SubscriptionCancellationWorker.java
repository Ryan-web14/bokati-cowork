package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionCancellationWorker {

    private final SubscriptionService subscriptionService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.cancellation-delay-ms:900000}")
    public void cancelEndedSubscriptions() {
        int cancelled = subscriptionService.cancelEndedSubscriptions();
        if (cancelled > 0) {
            log.info("Cancelled {} subscriptions at period end", cancelled);
        }
    }
}
