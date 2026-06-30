package com.sni.bokaticowork.features.billing.worker;

import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPaymentReminderWorker {

    private final BillingDocumentRepository documentRepository;
    private final BillingEmailService billingEmailService;

    @Scheduled(cron = "${bokati.billing.workers.payment-reminder-cron:0 0 8 * * *}")
    public void sendOverdueReminders() {
        try {
            int sent = 0;
            int failed = 0;
            for (var document : documentRepository.findOverdueReminderCandidates(100)) {
                try {
                    if (billingEmailService.sendDocument(document.getDocumentNumber())) {
                        sent++;
                    }
                } catch (Exception ex) {
                    failed++;
                    log.warn("Failed to send overdue reminder for document {}: {}",
                            document.getDocumentNumber(), ex.getMessage());
                }
            }
            if (sent > 0 || failed > 0) {
                log.info("Overdue billing reminders: {} sent, {} failed", sent, failed);
            }
        } catch (Exception ex) {
            log.error("BillingPaymentReminderWorker failed: {}", ex.getMessage(), ex);
        }
    }
}
