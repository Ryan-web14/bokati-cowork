package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.features.accounting.service.implementation.CommitmentService;
import com.sni.bokaticowork.features.accounting.service.implementation.LoanService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;

/**
 * Tenue quotidienne du financement (lot F17) : echeances d'emprunt ecrites seules a leur date,
 * alertes avant echeance, loyers restant dus des baux, expirations et alertes du hors bilan,
 * rapprochement du registre et des comptes 90.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancingWorker implements SchedulingConfigurer {

    private static final String CRON_KEY = "accounting.financing.cron";

    private SettingTriggers triggers;
    private ObjectProvider<FinancingWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<FinancingWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.cron(CRON_KEY, "0 45 6 * * *", ZoneId.of("Africa/Brazzaville")));
    }

    private final LoanService loans;
    private final CommitmentService commitments;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;

    public void run() {
        if (!settings.enabled()) {
            return;
        }
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Brazzaville"));
        try {
            log.info("Financement : {} echeance(s) d'emprunt ecrite(s), {} alerte(s)", loans.postDue(today), loans.alerts(today));
        } catch (RuntimeException exception) {
            log.warn("Financement : echeances d'emprunt en echec : {}", exception.getMessage());
            notifier.notifyAccountants("Echeances d'emprunt non ecrites", "L'ecriture automatique des echeances d'emprunt a echoue : "
                    + exception.getMessage(), null, "/accounting/loans", null);
        }
        try {
            log.info("Hors bilan : {}", commitments.refresh(today));
        } catch (RuntimeException exception) {
            log.warn("Hors bilan : tenue quotidienne en echec : {}", exception.getMessage());
        }
    }
}
