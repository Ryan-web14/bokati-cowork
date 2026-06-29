package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingOverdueWorker {

    private final BillingDocumentService billingDocumentService;
    private final AdminInAppNotifier adminInAppNotifier;

    @Scheduled(fixedDelayString = "${bokati.billing.workers.overdue-delay-ms:3600000}")
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
}
