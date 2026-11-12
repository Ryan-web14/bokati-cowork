package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;

/**
 * Marque dormants les portefeuilles sans mouvement.
 *
 * <p>La dormance ne ferme rien et ne bloque rien : elle note. Un portefeuille dormant qui bouge
 * se reveille de lui-meme, au premier mouvement, et ce reveil est signale a la revue · c'est la
 * seule consequence, et elle est voulue. Ce que devient un solde dormant a terme est une politique
 * ecrite, pas un automatisme.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletDormancyWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "bokati.wallet.dormancy.cron";
    private static final String AFTER_DAYS_KEY = "bokati.wallet.dormancy.after-days";

    private SettingTriggers triggers;
    private ObjectProvider<WalletDormancyWorker> self;
    private PlatformSettings platformSettings;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<WalletDormancyWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().markDormant(), triggers.cron(CRON_KEY, "0 15 3 * * *"));
    }

    private final WalletAccountRepository walletRepository;

    @Value("${bokati.wallet.dormancy.after-days:180}")
    private int dormancyDays;

    private long dormancyDays() {
        return platformSettings == null ? dormancyDays : platformSettings.number(AFTER_DAYS_KEY, dormancyDays);
    }

    @Transactional
    public void markDormant() {
        long dormancyDays = dormancyDays();
        Instant before = Instant.now().minus(Duration.ofDays(Math.max(1, dormancyDays)));
        List<Long> candidates = walletRepository.findDormantCandidates(before);
        int marked = 0;
        for (Long id : candidates) {
            WalletAccount wallet = walletRepository.findById(id).orElse(null);
            if (wallet == null || wallet.getDormantSince() != null) {
                continue;
            }
            wallet.setDormantSince(Instant.now());
            walletRepository.save(wallet);
            marked++;
        }
        if (marked > 0) {
            log.info("Portefeuille · {} compte(s) marque(s) dormant(s) apres {} jours sans mouvement", marked, dormancyDays);
        }
    }
}
