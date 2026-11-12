package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.features.accounting.service.implementation.ExchangeRateService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import java.time.ZoneId;

/** Recuperation quotidienne des taux de change sur internet (gestion multidevise). */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateWorker implements SchedulingConfigurer {

    private static final String FETCH_CRON_KEY = "accounting.exchange.fetch-cron";

    private SettingTriggers triggers;
    private ObjectProvider<ExchangeRateWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<ExchangeRateWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().fetch(), triggers.cron(FETCH_CRON_KEY, "0 15 7 * * *", ZoneId.of("Africa/Brazzaville")));
    }

    private final ExchangeRateService rates;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;

    public void fetch() {
        if ("false".equalsIgnoreCase(settings.text("accounting.exchange.auto-fetch", "true"))) {
            return;
        }
        try {
            log.info("Taux de change : {} taux recuperes", rates.fetch().size());
        } catch (RuntimeException exception) {
            log.warn("Taux de change : recuperation en echec : {}", exception.getMessage());
            notifier.notifyAccountants("Taux de change non recuperes", "La recuperation automatique des taux du jour a echoue : "
                    + exception.getMessage() + ". Saisissez les taux a la main si une operation en devise est attendue.", null,
                    "/accounting/exchange-rates", null);
        }
    }
}
