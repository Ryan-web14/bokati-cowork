package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingOverdueWorker {

    private final BillingDocumentService billingDocumentService;

    @Scheduled(fixedDelayString = "${bokati.billing.workers.overdue-delay-ms:3600000}")
    public void markOverdueDocuments() {
        int marked = billingDocumentService.markOverdueDocuments();
        if (marked > 0) {
            log.info("Marked {} billing documents as overdue", marked);
        }
    }
}
