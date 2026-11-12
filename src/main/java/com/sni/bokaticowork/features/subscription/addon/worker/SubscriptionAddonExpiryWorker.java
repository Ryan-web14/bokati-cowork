package com.sni.bokaticowork.features.subscription.addon.worker;

import com.sni.bokaticowork.features.subscription.addon.service.SubscriptionAddonService;
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
public class SubscriptionAddonExpiryWorker implements SchedulingConfigurer {

    private static final String ADDON_EXPIRY_DELAY_MS_KEY = "bokati.subscription.workers.addon-expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SubscriptionAddonExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SubscriptionAddonExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expireEndedAddons(), triggers.fixedDelay(ADDON_EXPIRY_DELAY_MS_KEY, 900000L));
    }

    private final SubscriptionAddonService addonService;

    public void expireEndedAddons() {
        try {
            int expired = addonService.expireEndedAddons();
            if (expired > 0) {
                log.info("Expired {} subscription add-ons", expired);
            }
        } catch (Exception ex) {
            log.error("SubscriptionAddonExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
