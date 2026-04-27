package com.sni.bokaticowork.features.payment.worker;

import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntentExpiryWorker {

    private final PaymentIntentRepository paymentIntentRepository;

    @Scheduled(fixedDelayString = "${bokati.payment.workers.intent-expiry-delay-ms:300000}")
    public void expirePaymentIntents() {
        int expired = paymentIntentRepository.expirePending(Instant.now());
        if (expired > 0) {
            log.info("Expired {} payment intents", expired);
        }
    }
}
