package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassRenewalWorker implements SchedulingConfigurer {

    private static final String RENEWAL_DELAY_MS_KEY = "bokati.pass.workers.renewal-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<PassRenewalWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<PassRenewalWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().renewDuePasses(), triggers.fixedDelay(RENEWAL_DELAY_MS_KEY, 900000L));
    }

    private final PassService passService;

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
