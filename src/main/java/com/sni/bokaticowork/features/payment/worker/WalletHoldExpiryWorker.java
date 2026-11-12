package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
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
public class WalletHoldExpiryWorker implements SchedulingConfigurer {

    private static final String WALLET_HOLD_EXPIRY_DELAY_MS_KEY = "bokati.payment.workers.wallet-hold-expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<WalletHoldExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<WalletHoldExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expireWalletHolds(), triggers.fixedDelay(WALLET_HOLD_EXPIRY_DELAY_MS_KEY, 300000L));
    }

    private final WalletHoldService walletHoldService;

    public void expireWalletHolds() {
        int expired = walletHoldService.expireDueHolds();
        if (expired > 0) {
            log.info("Expired {} wallet holds", expired);
        }
    }
}
