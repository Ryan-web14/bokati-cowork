package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.transfer.service.WalletPaymentRequestService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

/**
 * Ferme ce qui n'a pas abouti · transferts jamais confirmes, demandes de paiement echues.
 *
 * <p>Ni les uns ni les autres ne bloquent d'argent : un transfert non confirme n'a rien debite, une
 * demande n'a rien retenu. Ce passage sert la lisibilite de l'historique, pas la securite · la
 * confirmation verifie deja l'echeance a la volee.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletUsageExpiryWorker implements SchedulingConfigurer {

    private static final String EXPIRY_DELAY_MS_KEY = "bokati.wallet.usage.expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<WalletUsageExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<WalletUsageExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expire(), triggers.fixedDelay(EXPIRY_DELAY_MS_KEY, 600000L));
    }

    private final WalletTransferService transferService;
    private final WalletPaymentRequestService paymentRequestService;

    public void expire() {
        int transfers = transferService.expireStale();
        int requests = paymentRequestService.expireStale();
        if (transfers > 0 || requests > 0) {
            log.info("Portefeuille · {} transfert(s) non confirme(s) et {} demande(s) echue(s) fermes", transfers, requests);
        }
    }
}
