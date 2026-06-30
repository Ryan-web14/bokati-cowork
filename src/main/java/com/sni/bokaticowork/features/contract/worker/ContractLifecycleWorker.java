package com.sni.bokaticowork.features.contract.worker;

import com.sni.bokaticowork.features.contract.service.interfaces.ContractLifecycleAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractLifecycleWorker {

    private final ContractLifecycleAutomationService automationService;

    @Value("${app.contract.worker.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${app.contract.worker.fixed-delay-ms:3600000}")
    public void process() {
        if (!enabled) {
            return;
        }
        try {
            int processed = automationService.processScheduledTransitions();
            if (processed > 0) {
                log.info("Processed {} contract lifecycle transition(s)", processed);
            }
        } catch (Exception ex) {
            log.error("ContractLifecycleWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
