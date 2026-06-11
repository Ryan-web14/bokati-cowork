package com.sni.bokaticowork.features.document.retention.worker;

import com.sni.bokaticowork.features.document.retention.service.interfaces.DocumentRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentRetentionWorker {

    private final DocumentRetentionService retentionService;

    @Value("${app.document.retention.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${app.document.retention.cron:0 30 2 * * *}")
    public void process() {
        if (!enabled) return;
        int archived = retentionService.applyPolicies();
        if (archived > 0) {
            log.info("Retention worker archived {} document(s)", archived);
        }
    }
}
