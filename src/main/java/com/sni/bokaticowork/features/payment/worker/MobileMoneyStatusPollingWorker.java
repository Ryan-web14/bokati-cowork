package com.sni.bokaticowork.features.payment.worker;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayProperties;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayCallbackProcessor;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Polls PawaPay for PROCESSING mobile money transactions that have not received
 * a callback within the configured time window. Handles stuck transactions due to
 * network issues, missed webhooks, or operator delays.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bokati.payment.pawaypay.enabled", havingValue = "true")
public class MobileMoneyStatusPollingWorker {

    private final PaymentTransactionRepository transactionRepository;
    private final MobileMoneyPaymentProvider mobileMoneyProvider;
    private final PawapayCallbackProcessor callbackProcessor;
    private final PawapayDepositService depositService;
    private final PawapayProperties properties;

    @Scheduled(fixedDelayString = "${bokati.payment.pawaypay.polling-delay-ms:300000}")
    public void pollProcessingTransactions() {
        Instant cutoff = Instant.now().minus(properties.getPollingMaxAgeMinutes(), ChronoUnit.MINUTES);
        List<PaymentTransaction> stuck = transactionRepository.findProcessingMobileMoneyTransactions(cutoff);

        if (stuck.isEmpty()) return;

        log.info("Polling {} stuck mobile money transaction(s)", stuck.size());

        for (PaymentTransaction transaction : stuck) {
            try {
                poll(transaction);
            } catch (Exception ex) {
                log.error("Error polling transaction {}", transaction.getTransactionNumber(), ex);
            }
        }
    }

    private void poll(PaymentTransaction transaction) {
        String depositId = transaction.getProviderReference();
        if (!StringUtils.hasText(depositId)) return;

        MobileMoneyStatusResponse status = mobileMoneyProvider.checkStatus(depositId);
        PawapayDeposit deposit = depositService.markStatusChecked(depositId, status.message());
        log.debug("Poll result for transaction {}: status={}", transaction.getTransactionNumber(), status.status());

        if ("SUCCEEDED".equals(status.status())) {
            // Synthesize a COMPLETED callback and reuse the existing processor logic
            callbackProcessor.process(new PawapayCallbackPayload(
                    depositId, "COMPLETED", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            ));
        } else if ("FAILED".equals(status.status())) {
            callbackProcessor.process(new PawapayCallbackPayload(
                    depositId, "FAILED", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null
            ));
        } else {
            // Still PROCESSING at the operator. Give up once we have exhausted the retry
            // budget so we stop polling the same stuck deposit forever and settle it FAILED.
            int attempts = deposit != null ? deposit.getStatusCheckCount() : 0;
            if (attempts >= properties.getMaxPollingAttempts()) {
                log.warn("Abandoning mobile money transaction {} as FAILED after {} status check(s)",
                        transaction.getTransactionNumber(), attempts);
                callbackProcessor.process(new PawapayCallbackPayload(
                        depositId, "FAILED", null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        TextNode.valueOf("Abandoned after " + attempts + " status checks without resolution")
                ));
            }
            // else: leave it for the next poll cycle
        }
    }
}
