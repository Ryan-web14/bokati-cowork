package com.sni.bokaticowork.features.subscription.notification.worker;

import com.sni.bokaticowork.features.subscription.notification.service.SubscriptionNotificationService;
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
public class SubscriptionNotificationDispatchWorker implements SchedulingConfigurer {

    private static final String NOTIFICATION_DISPATCH_DELAY_MS_KEY = "bokati.subscription.workers.notification-dispatch-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SubscriptionNotificationDispatchWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SubscriptionNotificationDispatchWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().dispatchDueNotifications(), triggers.fixedDelay(NOTIFICATION_DISPATCH_DELAY_MS_KEY, 60000L));
    }

    private final SubscriptionNotificationService notificationService;

    public void dispatchDueNotifications() {
        try {
            int dispatched = notificationService.dispatchDue(100);
            if (dispatched > 0) {
                log.info("Dispatched {} subscription notifications", dispatched);
            }
        } catch (Exception ex) {
            log.error("SubscriptionNotificationDispatchWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
