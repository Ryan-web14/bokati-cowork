package com.sni.bokaticowork.features.notification.worker;

import com.sni.bokaticowork.features.notification.service.interfaces.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliveryWorker {

    private final WebhookService webhookService;

    @Value("${bokati.webhook.worker.enabled:true}")
    private boolean enabled;

    @Value("${bokati.webhook.worker.batch-size:25}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${bokati.webhook.worker.delay-ms:60000}")
    public void process() {
        if (!enabled) {
            return;
        }
        int processed = webhookService.processPending(batchSize);
        if (processed > 0) {
            log.info("Processed {} webhook delivery(ies)", processed);
        }
    }
}
