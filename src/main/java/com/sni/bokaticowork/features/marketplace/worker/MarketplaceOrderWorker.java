package com.sni.bokaticowork.features.marketplace.worker;

import com.sni.bokaticowork.features.marketplace.service.MarketplaceOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Fait avancer les commandes · reglees par un chemin qui ne previent pas la commande (caisse,
 * portefeuille de l'organisation), echues, ou dont un article reste a sortir du stock.
 *
 * <p>Une transaction par commande · un echec n'arrete pas le lot.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketplaceOrderWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.marketplace.sync-delay-ms";
    private static final long DEFAULT_DELAY_MS = 60_000L;
    /** Lu une seule fois, au demarrage. */
    private static final String INITIAL_DELAY_KEY = "bokati.marketplace.sync-initial-delay-ms";
    private static final long DEFAULT_INITIAL_DELAY_MS = 60_000L;

    private SettingTriggers triggers;
    private ObjectProvider<MarketplaceOrderWorker> self;
    private PlatformSettings platformSettings;

    private static final int BATCH = 100;

    private final MarketplaceOrderService orderService;

    public void syncPendingOrders() {
        int changed = 0;
        for (String orderNumber : orderService.numbersToSync(BATCH)) {
            try {
                if (orderService.sync(orderNumber)) {
                    changed++;
                }
            } catch (RuntimeException ex) {
                log.warn("Commande {} · mise a jour impossible, reprise au prochain passage : {}", orderNumber, ex.getMessage());
            }
        }
        if (changed > 0) {
            log.info("{} commande(s) de la marketplace mise(s) a jour", changed);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<MarketplaceOrderWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        long initialDelayMs = platformSettings == null
                ? DEFAULT_INITIAL_DELAY_MS : platformSettings.number(INITIAL_DELAY_KEY, DEFAULT_INITIAL_DELAY_MS);
        registrar.addTriggerTask(() -> self.getObject().syncPendingOrders(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS, initialDelayMs));
    }
}
