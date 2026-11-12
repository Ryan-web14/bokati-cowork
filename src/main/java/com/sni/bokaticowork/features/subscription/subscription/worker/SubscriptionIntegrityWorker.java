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
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionIntegrityWorker implements SchedulingConfigurer {

    private static final String INTEGRITY_DELAY_MS_KEY = "bokati.subscription.workers.integrity-delay-ms";
    private static final String CONTRACT_CATCH_UP_INITIAL_DELAY_MS_KEY = "bokati.subscription.workers.contract-catch-up-initial-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<SubscriptionIntegrityWorker> self;
    private PlatformSettings platformSettings;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<SubscriptionIntegrityWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().repairActiveSubscriptionsWithoutGrants(), triggers.fixedDelay(INTEGRITY_DELAY_MS_KEY, 1800000L));
        registrar.addTriggerTask(() -> self.getObject().catchUpContractTerms(), triggers.fixedDelay(INTEGRITY_DELAY_MS_KEY, 1800000L, platformSettings == null ? 120000L : platformSettings.number(CONTRACT_CATCH_UP_INITIAL_DELAY_MS_KEY, 120000L)));
    }

    private final SubscriptionService subscriptionService;

    public void repairActiveSubscriptionsWithoutGrants() {
        try {
            int repaired = subscriptionService.repairActiveSubscriptionsWithoutGrants();
            if (repaired > 0) {
                log.warn("Repaired {} active subscriptions without entitlement grants", repaired);
            }
        } catch (Exception ex) {
            log.error("SubscriptionIntegrityWorker failed: {}", ex.getMessage(), ex);
        }
    }

    public void catchUpContractTerms() {
        try {
            int caughtUp = subscriptionService.catchUpContractTerms();
            if (caughtUp > 0) {
                log.info("{} contrat(s) d'abonnement prolonge(s) par avenant", caughtUp);
            }
        } catch (Exception ex) {
            log.error("Rattrapage des contrats d'abonnement impossible : {}", ex.getMessage(), ex);
        }
    }
}
