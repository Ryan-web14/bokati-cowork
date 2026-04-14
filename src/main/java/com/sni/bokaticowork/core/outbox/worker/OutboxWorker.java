package com.sni.bokaticowork.core.outbox.worker;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxService outboxService;

    @Value("${app.outbox.worker.enabled:true}")
    private boolean enabled;

    @Value("${app.outbox.worker.batch-size:25}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.worker.fixed-delay-ms:10000}")
    public void process() {
        if (!enabled) {
            return;
        }

        int processed = outboxService.processPending(batchSize);
        if (processed > 0) {
            log.info("Processed {} outbox event(s)", processed);
        }
    }
}
