package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.features.accounting.service.implementation.ReceivableRiskService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creances a risque (lot F19), chaque nuit : propositions de classement, factures passees en perte
 * dans la facturation soldees en comptabilite, ajustement mensuel des depreciations le 1er du mois,
 * controles signales aux comptables.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProvisionWorker implements SchedulingConfigurer {

    static final String CRON_KEY = "accounting.receivable.cron";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final ReceivableRiskService receivables;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;

    private SettingTriggers triggers;
    private ObjectProvider<ProvisionWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<ProvisionWorker> self) {
        this.self = self;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.cron(CRON_KEY, "0 15 4 * * *", ZONE));
    }

    public void run() {
        if (!settings.enabled()) {
            return;
        }
        LocalDate today = LocalDate.now(ZONE);
        try {
            log.info("Creances : {} proposition(s) de classement", receivables.propose(today));
        } catch (RuntimeException exception) {
            log.warn("Creances : propositions en echec : {}", exception.getMessage());
        }
        try {
            int synced = receivables.syncBillingWriteOffs(today);
            if (synced > 0) {
                log.info("Creances : {} perte(s) de la facturation soldee(s) en comptabilite", synced);
            }
        } catch (RuntimeException exception) {
            log.warn("Creances : synchronisation des pertes en echec : {}", exception.getMessage());
        }
        if (today.getDayOfMonth() == 1 && "MONTHLY".equalsIgnoreCase(settings.text("accounting.receivable.adjustment", "MONTHLY").trim())) {
            try {
                ReceivableRiskService.ImpairmentRun run = receivables.runImpairmentAutomatically(YearMonth.from(today).minusMonths(1));
                log.info("Creances : ajustement {} : dotations {}, reprises {}", run.period(), run.allowances(), run.releases());
            } catch (RuntimeException exception) {
                log.warn("Creances : ajustement des depreciations en echec : {}", exception.getMessage());
                notifier.notifyAccountants("Ajustement des depreciations de creances non passe", exception.getMessage(), null,
                        "/accounting/receivables/impairment-runs", null);
            }
        }
        try {
            List<ReceivableRiskService.Finding> blocking = receivables.controls(today).stream()
                    .filter(finding -> "BLOCKING".equals(finding.severity())).toList();
            if (!blocking.isEmpty()) {
                notifier.notifyAccountants("Creances a risque : ecarts de controle",
                        blocking.stream().map(finding -> finding.subject() + " : " + finding.message()).collect(Collectors.joining("\n")),
                        null, "/accounting/receivables/controls", null);
            }
        } catch (RuntimeException exception) {
            log.warn("Creances : controles en echec : {}", exception.getMessage());
        }
    }
}
