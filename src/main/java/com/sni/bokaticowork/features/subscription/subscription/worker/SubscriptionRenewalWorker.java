package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
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
public class SubscriptionRenewalWorker implements SchedulingConfigurer {

    private static final String RENEWAL_DELAY_MS_KEY = "bokati.subscription.workers.renewal-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SubscriptionRenewalWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SubscriptionRenewalWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().renewDueSubscriptions(), triggers.fixedDelay(RENEWAL_DELAY_MS_KEY, 900000L));
    }

    private final SubscriptionService subscriptionService;

    public void renewDueSubscriptions() {
        try {
            int renewed = subscriptionService.renewDueSubscriptions();
            if (renewed > 0) {
                log.info("Renewed {} due subscriptions", renewed);
            }
        } catch (Exception ex) {
            log.error("SubscriptionRenewalWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
