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
public class PassExpiryWorker implements SchedulingConfigurer {

    private static final String PASS_EXPIRY_DELAY_MS_KEY = "bokati.subscription.workers.pass-expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<PassExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<PassExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expirePasses(), triggers.fixedDelay(PASS_EXPIRY_DELAY_MS_KEY, 600000L));
    }

    private final PassService passService;

    public void expirePasses() {
        try {
            int expired = passService.expirePasses();
            if (expired > 0) {
                log.info("Expired {} passes", expired);
            }
        } catch (Exception ex) {
            log.error("PassExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
