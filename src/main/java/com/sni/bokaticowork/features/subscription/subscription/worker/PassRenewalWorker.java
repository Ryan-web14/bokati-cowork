package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassRenewalWorker {

    private final PassService passService;

    @Scheduled(fixedDelayString = "${bokati.pass.workers.renewal-delay-ms:900000}")
    public void renewDuePasses() {
        try {
            int renewed = passService.renewDuePasses();
            if (renewed > 0) {
                log.info("Renewed {} due passes", renewed);
            }
        } catch (Exception ex) {
            log.error("PassRenewalWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
