package com.sni.bokaticowork.features.subscription.rollover.worker;

import com.sni.bokaticowork.features.subscription.rollover.service.SubscriptionRolloverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRolloverWorker {

    private final SubscriptionRolloverService rolloverService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.rollover-delay-ms:900000}")
    public void applyDueRollovers() {
        int applied = rolloverService.applyDueRollovers();
        if (applied > 0) {
            log.info("Applied {} subscription rollovers", applied);
        }
    }
}
