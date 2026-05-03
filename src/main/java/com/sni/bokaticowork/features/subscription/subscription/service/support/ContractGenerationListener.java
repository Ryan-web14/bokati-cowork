package com.sni.bokaticowork.features.subscription.subscription.service.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractGenerationListener {

    private final ContractGenerationProcessor processor;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onContractGenerationRequested(ContractGenerationEvent event) {
        try {
            processor.process(event);
        } catch (Exception ex) {
            log.error("Async contract generation failed for {} id={}: {}",
                    event.sourceType(), event.sourceId(), ex.getMessage(), ex);
        }
    }
}
