package com.sni.bokaticowork.features.notification.worker;

import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationService notificationService;

    @Value("${bokati.notification.worker.enabled:true}")
    private boolean enabled;

    @Value("${bokati.notification.worker.batch-size:25}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${bokati.notification.worker.delay-ms:30000}")
    public void process() {
        if (!enabled) {
            return;
        }
        try {
            int processed = notificationService.processPending(batchSize);
            if (processed > 0) {
                log.info("Processed {} notification(s)", processed);
            }
        } catch (Exception ex) {
            log.error("NotificationWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
