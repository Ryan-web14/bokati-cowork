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
public class EntitlementReservationExpiryWorker implements SchedulingConfigurer {

    private static final String RESERVATION_EXPIRY_DELAY_MS_KEY = "bokati.subscription.workers.reservation-expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<EntitlementReservationExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<EntitlementReservationExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expireReservations(), triggers.fixedDelay(RESERVATION_EXPIRY_DELAY_MS_KEY, 300000L));
    }

    private final EntitlementService entitlementService;

    public void expireReservations() {
        try {
            int expired = entitlementService.expireReservations();
            if (expired > 0) {
                log.info("Expired {} entitlement reservations", expired);
            }
        } catch (Exception ex) {
            log.error("EntitlementReservationExpiryWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
