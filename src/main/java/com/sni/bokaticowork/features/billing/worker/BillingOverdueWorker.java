package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingOverdueWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.billing.workers.overdue-delay-ms";
    private static final long DEFAULT_DELAY_MS = 3_600_000L;

    private SettingTriggers triggers;
    private ObjectProvider<BillingOverdueWorker> self;

    private final BillingDocumentService billingDocumentService;
    private final AdminInAppNotifier adminInAppNotifier;

    public void markOverdueDocuments() {
        int marked = billingDocumentService.markOverdueDocuments();
        if (marked > 0) {
            log.info("Marked {} billing documents as overdue", marked);
            adminInAppNotifier.broadcastAlert(
                    "BILLING_OVERDUE", "BILLING",
                    marked + " facture(s) en retard",
                    marked + " document(s) de facturation marqué(s) en retard de paiement",
                    "WARNING", null
            );
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<BillingOverdueWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().markOverdueDocuments(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
