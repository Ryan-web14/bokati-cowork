package com.sni.bokaticowork.features.payment.service.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTransactionWorkflowListener {

    private final PaymentTransactionWorkflowProcessor processor;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentTransactionWorkflow(PaymentTransactionWorkflowEvent event) {
        try {
            processor.process(event);
        } catch (Exception ex) {
            log.error("Failed to process asynchronous payment workflow for transaction {}", event.transactionNumber(), ex);
        }
    }
}
