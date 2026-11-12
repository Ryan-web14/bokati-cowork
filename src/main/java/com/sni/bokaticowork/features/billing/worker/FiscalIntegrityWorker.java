package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.fiscal.FiscalIntegrityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FiscalIntegrityWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "bokati.billing.workers.fiscal-integrity-cron";
    private static final String DEFAULT_CRON = "0 0 2 * * *";

    private SettingTriggers triggers;
    private ObjectProvider<FiscalIntegrityWorker> self;

    private final FiscalIntegrityCheckService integrityCheckService;

    public void runIntegrityCheck() {
        log.info("Starting nightly SEFC fiscal integrity check");
        FiscalIntegrityCheckService.FiscalIntegrityReport report =
                integrityCheckService.checkAndSave("SCHEDULER");
        log.info("SEFC integrity check complete · valid:{} checked:{} chains:{} sigs:{} gaps:{}",
                report.valid(), report.checkedInvoices(),
                report.brokenChains(), report.missingSignatures(), report.numberingGaps());
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<FiscalIntegrityWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().runIntegrityCheck(), triggers.cron(CRON_KEY, DEFAULT_CRON));
    }
}
