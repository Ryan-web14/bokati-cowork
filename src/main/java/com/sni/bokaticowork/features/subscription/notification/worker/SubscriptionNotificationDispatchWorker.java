package com.sni.bokaticowork.features.subscription.notification.worker;

import com.sni.bokaticowork.features.subscription.notification.service.SubscriptionNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionNotificationDispatchWorker {

    private final SubscriptionNotificationService notificationService;

    @Scheduled(fixedDelayString = "${bokati.subscription.workers.notification-dispatch-delay-ms:60000}")
    public void dispatchDueNotifications() {
        try {
            int dispatched = notificationService.dispatchDue(100);
            if (dispatched > 0) {
                log.info("Dispatched {} subscription notifications", dispatched);
            }
        } catch (Exception ex) {
            log.error("SubscriptionNotificationDispatchWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
