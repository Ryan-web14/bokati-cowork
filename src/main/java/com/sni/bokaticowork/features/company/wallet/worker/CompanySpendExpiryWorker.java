package com.sni.bokaticowork.features.company.wallet.worker;

import com.sni.bokaticowork.features.company.wallet.service.CompanyWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Annule les depenses que le gerant n'a pas validees dans le delai, et rend leurs fonds.
 *
 * <p>Une transaction par depense · un echec n'arrete pas le lot, la depense est reprise au
 * passage suivant.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanySpendExpiryWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.company.wallet.approval-expiry-delay-ms";
    private static final long DEFAULT_DELAY_MS = 600_000L;

    private SettingTriggers triggers;
    private ObjectProvider<CompanySpendExpiryWorker> self;

    private static final int BATCH = 50;

    private final CompanyWalletService walletService;

    public void expirePendingSpends() {
        int expired = 0;
        for (Long spendId : walletService.expiredPendingIds(BATCH)) {
            try {
                if (walletService.expire(spendId)) {
                    expired++;
                }
            } catch (RuntimeException ex) {
                log.warn("Depense {} · expiration impossible, reprise au prochain passage : {}", spendId, ex.getMessage());
            }
        }
        if (expired > 0) {
            log.info("{} depense(s) sans validation du gerant annulee(s), fonds rendus", expired);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<CompanySpendExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expirePendingSpends(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
