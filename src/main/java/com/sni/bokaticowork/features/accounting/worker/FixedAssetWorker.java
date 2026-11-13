package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.features.accounting.service.implementation.DepreciationRunService;
import com.sni.bokaticowork.features.accounting.service.implementation.FixedAssetReportService;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Immobilisations (lot F18) : dotations automatiques au rythme choisi (mensuel par defaut, le 1er
 * a 3 h) et controles quotidiens du fichier contre la comptabilite, signales aux comptables.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FixedAssetWorker implements SchedulingConfigurer {

    static final String RUN_CRON_KEY = "accounting.fixed-asset.depreciation-cron";
    static final String CONTROLS_CRON_KEY = "accounting.fixed-asset.controls-cron";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final DepreciationRunService runs;
    private final FixedAssetReportService reports;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;

    private SettingTriggers triggers;
    private ObjectProvider<FixedAssetWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<FixedAssetWorker> self) {
        this.self = self;
    }

    /** Planifications relues a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().depreciate(), triggers.cron(RUN_CRON_KEY, "0 0 3 1 * *", ZONE));
        registrar.addTriggerTask(() -> self.getObject().controls(), triggers.cron(CONTROLS_CRON_KEY, "0 30 6 * * *", ZONE));
    }

    /** Dotations des mois ecoules selon le rythme : mensuel chaque mois, trimestriel et annuel au debut de la periode. */
    public void depreciate() {
        if (!settings.enabled()) {
            return;
        }
        LocalDate today = LocalDate.now(ZONE);
        String rhythm = settings.text("accounting.fixed-asset.rhythm", "MONTHLY").trim().toUpperCase(Locale.ROOT);
        int months = switch (rhythm) {
            case "QUARTERLY" -> today.getMonthValue() % 3 == 1 ? 3 : 0;
            case "YEARLY" -> today.getMonthValue() == 1 ? 12 : 0;
            default -> 1;
        };
        for (int back = months - 1; back >= 0; back--) {
            LocalDate reference = today.minusMonths(back);
            try {
                DepreciationRunService.RunResult result = runs.runAutomatically(reference);
                log.info("Immobilisations : dotations {} sur {} bien(s), total {}", result.period(), result.assets(), result.total());
            } catch (RuntimeException exception) {
                log.warn("Immobilisations : dotations automatiques en echec : {}", exception.getMessage());
                notifier.notifyAccountants("Dotations aux amortissements non passees",
                        "Le traitement automatique des dotations a echoue : " + exception.getMessage(), null,
                        "/accounting/fixed-assets/depreciation-runs", null);
                return;
            }
        }
    }

    /** Controles quotidiens : un ecart bloquant est signale aux comptables. */
    public void controls() {
        if (!settings.enabled()) {
            return;
        }
        try {
            List<FixedAssetReportService.Finding> findings = reports.controlsUnchecked(LocalDate.now(ZONE));
            List<FixedAssetReportService.Finding> blocking = findings.stream().filter(finding -> "BLOCKING".equals(finding.severity())).toList();
            log.info("Immobilisations : {} ecart(s) de controle dont {} bloquant(s)", findings.size(), blocking.size());
            if (!blocking.isEmpty()) {
                notifier.notifyAccountants("Fichier des immobilisations et comptabilite en ecart",
                        blocking.stream().map(finding -> finding.subject() + " : " + finding.message()).collect(Collectors.joining("\n")),
                        null, "/accounting/fixed-assets/controls", null);
            }
        } catch (RuntimeException exception) {
            log.warn("Immobilisations : controles en echec : {}", exception.getMessage());
        }
    }
}
