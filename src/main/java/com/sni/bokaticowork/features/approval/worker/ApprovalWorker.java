package com.sni.bokaticowork.features.approval.worker;

import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/** Rappels et escalades des demandes d'approbation de tous les centres (lot B15). */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "approval.worker-cron";

    private SettingTriggers triggers;
    private ObjectProvider<ApprovalWorker> self;

    private final ApprovalEngine engine;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<ApprovalWorker> self) {
        this.self = self;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.cron(CRON_KEY, "0 0 8-18 * * *", ApprovalEngine.ZONE));
    }

    public void run() {
        try {
            log.info("Approbations · relances : {}", engine.remind());
        } catch (RuntimeException exception) {
            log.error("Approbations · relances en echec : {}", exception.getMessage(), exception);
        }
    }
}
