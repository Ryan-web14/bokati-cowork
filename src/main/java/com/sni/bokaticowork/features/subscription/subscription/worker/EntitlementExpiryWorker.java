package com.sni.bokaticowork.features.subscription.subscription.worker;

import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
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
public class EntitlementExpiryWorker implements SchedulingConfigurer {

    private static final String ENTITLEMENT_EXPIRY_DELAY_MS_KEY = "bokati.subscription.workers.entitlement-expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<EntitlementExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<EntitlementExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expireGrants(), triggers.fixedDelay(ENTITLEMENT_EXPIRY_DELAY_MS_KEY, 600000L));
    }

    private final EntitlementService entitlementService;

    public void expireGrants() {
        try {
            int expired = entitlementService.expireGrants();
            if (expired > 0) {
                log.info("Expired {} entitlement grants", expired);
            }
        } catch (Exception ex) {
            log.error("EntitlementExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
