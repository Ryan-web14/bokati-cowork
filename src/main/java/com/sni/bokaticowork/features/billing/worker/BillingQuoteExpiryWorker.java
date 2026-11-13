package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/** Lot B1 : passe en EXPIRED les devis dont la date de validite est passee sans reponse du client. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingQuoteExpiryWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.billing.workers.quote-expiry-delay-ms";
    private static final long DEFAULT_DELAY_MS = 3_600_000L;

    private SettingTriggers triggers;
    private ObjectProvider<BillingQuoteExpiryWorker> self;

    private final BillingDocumentService billingDocumentService;
    private final AdminInAppNotifier adminInAppNotifier;

    public void expireQuotes() {
        int expired = billingDocumentService.expireQuotes();
        if (expired > 0) {
            log.info("Expired {} quotes past their validity date", expired);
            adminInAppNotifier.broadcastAlert(
                    "BILLING_QUOTE_EXPIRED", "BILLING",
                    expired + " devis expiré(s)",
                    expired + " devis sans réponse ont dépassé leur date de validité",
                    "INFO", null
            );
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<BillingQuoteExpiryWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().expireQuotes(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
