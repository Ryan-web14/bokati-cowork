package com.sni.bokaticowork.features.subscription.change.worker;

import com.sni.bokaticowork.features.subscription.change.service.SubscriptionChangeRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionChangeWorker {

    private final SubscriptionChangeRequestService changeRequestService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.change-delay-ms:900000}")
    public void applyDueChanges() {
        try {
            int applied = changeRequestService.applyDueChanges();
            if (applied > 0) {
                log.info("Applied {} due subscription changes", applied);
            }
        } catch (Exception ex) {
            log.error("SubscriptionChangeWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
