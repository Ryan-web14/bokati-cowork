package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.features.accounting.service.implementation.DeferralService;
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
 * Constatations d'avance (lot F20) : chaque nuit, echeanciers des nouvelles lignes ; le 1er du mois,
 * traitement de fin de periode du mois ecoule (produits puis charges) ; ecarts signales.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeferralWorker implements SchedulingConfigurer {

    static final String CRON_KEY = "accounting.deferral.cron";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final DeferralService deferrals;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;

    private SettingTriggers triggers;
    private ObjectProvider<DeferralWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<DeferralWorker> self) {
        this.self = self;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().run(), triggers.cron(CRON_KEY, "0 0 4 * * *", ZONE));
    }

    public void run() {
        if (!settings.enabled()) {
            return;
        }
        LocalDate today = LocalDate.now(ZONE);
        try {
            log.info("Constatations d'avance : {} echeancier(s) cree(s)", deferrals.sync());
        } catch (RuntimeException exception) {
            log.warn("Constatations d'avance : creation des echeanciers en echec : {}", exception.getMessage());
        }
        if (today.getDayOfMonth() == 1) {
            try {
                log.info("Constatations d'avance : {} estimation(s) proposee(s)", deferrals.proposeAccruals(YearMonth.from(today).minusMonths(1)));
            } catch (RuntimeException exception) {
                log.warn("Constatations d'avance : propositions d'estimations en echec : {}", exception.getMessage());
            }
            for (String kind : List.of(DeferralService.REVENUE, DeferralService.EXPENSE)) {
                try {
                    DeferralService.RunResult result = deferrals.runAutomatically(YearMonth.from(today).minusMonths(1), kind);
                    log.info("Constatations d'avance {} {} : besoin {}, difference {}", kind, result.period(), result.totalNeed(), result.difference());
                } catch (RuntimeException exception) {
                    log.warn("Constatations d'avance {} : traitement de fin de periode en echec : {}", kind, exception.getMessage());
                    notifier.notifyAccountants("Constatations d'avance non passees (" + kind + ")", exception.getMessage(), null,
                            "/accounting/deferral-runs", null);
                }
            }
        }
        try {
            List<DeferralService.Finding> blocking = deferrals.controls(today).stream().filter(item -> "BLOCKING".equals(item.severity())).toList();
            if (!blocking.isEmpty()) {
                notifier.notifyAccountants("Constatations d'avance : ecarts", blocking.stream().map(item -> item.subject() + " : " + item.message())
                        .collect(Collectors.joining("\n")), null, "/accounting/deferrals/controls", null);
            }
        } catch (RuntimeException exception) {
            log.warn("Constatations d'avance : controles en echec : {}", exception.getMessage());
        }
    }
}
