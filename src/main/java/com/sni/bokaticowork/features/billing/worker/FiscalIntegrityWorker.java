package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.fiscal.FiscalIntegrityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FiscalIntegrityWorker {

    private final FiscalIntegrityCheckService integrityCheckService;

    @Scheduled(cron = "${bokati.billing.workers.fiscal-integrity-cron:0 0 2 * * *}")
    public void runIntegrityCheck() {
        log.info("Starting nightly SEFC fiscal integrity check");
        FiscalIntegrityCheckService.FiscalIntegrityReport report =
                integrityCheckService.checkAndSave("SCHEDULER");
        log.info("SEFC integrity check complete — valid:{} checked:{} chains:{} sigs:{} gaps:{}",
                report.valid(), report.checkedInvoices(),
                report.brokenChains(), report.missingSignatures(), report.numberingGaps());
    }
}
