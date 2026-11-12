package com.sni.bokaticowork.features.fund.worker;

import com.sni.bokaticowork.features.fund.service.FundLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import java.time.ZoneId;

/** Rappels, escalades, expirations et relances de justification des demandes de fonds. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundWorker implements SchedulingConfigurer {

    private static final String WORKER_CRON_KEY = "fund.worker-cron";

    private SettingTriggers triggers;
    private ObjectProvider<FundWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<FundWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.cron(WORKER_CRON_KEY, "0 0 8-18 * * *", ZoneId.of("Africa/Brazzaville")));
    }

    private final FundLifecycleService lifecycle;

    public void run() {
        try {
            log.info("Demandes de fonds · suivi : {}", lifecycle.runPeriodic());
        } catch (RuntimeException exception) {
            log.error("Demandes de fonds · suivi en echec : {}", exception.getMessage(), exception);
        }
    }
}
