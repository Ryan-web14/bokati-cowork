package com.sni.bokaticowork.features.fund.worker;

import com.sni.bokaticowork.features.fund.service.FundLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Rappels, escalades, expirations et relances de justification des demandes de fonds. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundWorker {

    private final FundLifecycleService lifecycle;

    @Scheduled(cron = "${fund.worker-cron:0 0 8-18 * * *}", zone = "Africa/Brazzaville")
    public void run() {
        try {
            log.info("Demandes de fonds · suivi : {}", lifecycle.runPeriodic());
        } catch (RuntimeException exception) {
            log.error("Demandes de fonds · suivi en echec : {}", exception.getMessage(), exception);
        }
    }
}
