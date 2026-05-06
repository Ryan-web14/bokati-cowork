package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.model.PaymentDunningAttempt;
import com.sni.bokaticowork.features.payment.repository.PaymentDunningAttemptRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.DunningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DunningWorker {

    private final PaymentDunningAttemptRepository dunningRepo;
    private final DunningService dunningService;

    @Scheduled(fixedDelayString = "${bokati.payment.workers.dunning-delay-ms:60000}")
    public void executeDueAttempts() {
        List<PaymentDunningAttempt> due = dunningRepo.findDuePending(Instant.now());
        if (due.isEmpty()) return;

        log.info("Executing {} due dunning attempt(s)", due.size());
        for (PaymentDunningAttempt attempt : due) {
            try {
                dunningService.executeAttempt(attempt);
            } catch (Exception ex) {
                log.error("Unexpected error executing dunning attempt id={} for intent id={}",
                        attempt.getId(), attempt.getPaymentIntentId(), ex);
            }
        }
    }
}
