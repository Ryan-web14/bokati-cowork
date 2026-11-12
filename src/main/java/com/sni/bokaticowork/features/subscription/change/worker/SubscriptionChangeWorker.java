package com.sni.bokaticowork.features.subscription.change.worker;

import com.sni.bokaticowork.features.subscription.change.service.SubscriptionChangeRequestService;
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
public class SubscriptionChangeWorker implements SchedulingConfigurer {

    private static final String CHANGE_DELAY_MS_KEY = "bokati.subscription.workers.change-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SubscriptionChangeWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SubscriptionChangeWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().applyDueChanges(), triggers.fixedDelay(CHANGE_DELAY_MS_KEY, 900000L));
    }

    private final SubscriptionChangeRequestService changeRequestService;

    public void applyDueChanges() {
        try {
            int applied = changeRequestService.applyDueChanges();
            if (applied > 0) {
                log.info("Applied {} due subscription changes", applied);
            }
        } catch (Exception ex) {
            log.error("SubscriptionChangeWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
