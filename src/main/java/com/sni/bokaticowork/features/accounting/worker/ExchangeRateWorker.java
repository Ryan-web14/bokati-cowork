package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.features.accounting.service.implementation.ExchangeRateService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Recuperation quotidienne des taux de change sur internet (gestion multidevise). */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateWorker {

    private final ExchangeRateService rates;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;

    @Scheduled(cron = "${accounting.exchange.fetch-cron:0 15 7 * * *}", zone = "Africa/Brazzaville")
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
