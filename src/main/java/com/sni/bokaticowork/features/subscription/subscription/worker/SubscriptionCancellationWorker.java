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
public class SubscriptionCancellationWorker implements SchedulingConfigurer {

    private static final String CANCELLATION_DELAY_MS_KEY = "bokati.subscription.workers.cancellation-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SubscriptionCancellationWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SubscriptionCancellationWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().cancelEndedSubscriptions(), triggers.fixedDelay(CANCELLATION_DELAY_MS_KEY, 900000L));
    }

    private final SubscriptionService subscriptionService;

    public void cancelEndedSubscriptions() {
        try {
            int cancelled = subscriptionService.cancelEndedSubscriptions();
            if (cancelled > 0) {
                log.info("Cancelled {} subscriptions at period end", cancelled);
            }
        } catch (Exception ex) {
            log.error("SubscriptionCancellationWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
