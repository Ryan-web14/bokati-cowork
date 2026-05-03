package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.dto.request.CreateReconciliationBatchRequest;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationWorker {

    private final PaymentReconciliationService reconciliationService;

    @Scheduled(cron = "${bokati.payment.workers.reconciliation-cron:0 30 2 * * *}")
    public void reconcileDaily() {
        var batch = reconciliationService.create(new CreateReconciliationBatchRequest("AUTO", "SYSTEM"));
        reconciliationService.complete(batch.batchNumber());
        log.info("Payment reconciliation batch completed: {}", batch.batchNumber());
    }
}
