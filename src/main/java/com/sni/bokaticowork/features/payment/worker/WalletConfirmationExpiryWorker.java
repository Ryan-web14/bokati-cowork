package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.security.service.WalletConfirmationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

/**
 * Ferme les demandes de confirmation echues.
 *
 * <p>La confirmation verifie deja l'echeance a la volee : une demande perimee ne peut pas etre
 * utilisee, worker ou pas. Ce passage ne sert donc pas a la securite mais a la lisibilite · sans
 * lui, une table pleine de {@code PENDING} qui ne le sont plus depuis des semaines rendrait toute
 * lecture du journal de confirmation trompeuse, et une alerte sur « les confirmations en attente »
 * impossible a regler.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletConfirmationExpiryWorker implements SchedulingConfigurer {

    private static final String EXPIRY_DELAY_MS_KEY = "bokati.wallet.confirmation.expiry-delay-ms";

    private SettingTriggers triggers;
    private ObjectProvider<WalletConfirmationExpiryWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<WalletConfirmationExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expire(), triggers.fixedDelay(EXPIRY_DELAY_MS_KEY, 300000L));
    }

    private final WalletConfirmationService confirmationService;

    public void expire() {
        int closed = confirmationService.expireStale();
        if (closed > 0) {
            log.info("Portefeuille · {} demande(s) de confirmation echue(s) fermee(s)", closed);
        }
    }
}
