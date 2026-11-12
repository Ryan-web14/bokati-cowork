package com.sni.bokaticowork.features.subscription.rollover.worker;

import com.sni.bokaticowork.features.subscription.rollover.service.SubscriptionRolloverService;
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
public class SubscriptionRolloverWorker implements SchedulingConfigurer {

    private static final String ROLLOVER_DELAY_MS_KEY = "bokati.subscription.workers.rollover-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SubscriptionRolloverWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SubscriptionRolloverWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().applyDueRollovers(), triggers.fixedDelay(ROLLOVER_DELAY_MS_KEY, 900000L));
    }

    private final SubscriptionRolloverService rolloverService;

    public void applyDueRollovers() {
        try {
            int applied = rolloverService.applyDueRollovers();
            if (applied > 0) {
                log.info("Applied {} subscription rollovers", applied);
            }
        } catch (Exception ex) {
            log.error("SubscriptionRolloverWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
