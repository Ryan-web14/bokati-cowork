package com.sni.bokaticowork.features.document.documentMaster.worker;

import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentLifecycleAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentLifecycleWorker {

    private final DocumentLifecycleAutomationService automationService;

    @Value("${app.document.worker.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${app.document.worker.fixed-delay-ms:3600000}")
    public void process() {
        if (!enabled) {
            return;
        }
        try {
            int processed = automationService.expireDocuments();
            if (processed > 0) {
                log.info("Expired {} document(s) automatically", processed);
            }
            int reminded = automationService.notifyPreExpiry();
            if (reminded > 0) {
                log.info("Sent {} document pre-expiry reminder(s)", reminded);
            }
            int autoApproved = automationService.autoApproveDocuments();
            if (autoApproved > 0) {
                log.info("Auto-approved {} document(s)", autoApproved);
            }
        } catch (Exception ex) {
            log.error("DocumentLifecycleWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
