package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.features.payment.enums.DunningAttemptStatus;
import com.sni.bokaticowork.features.payment.model.PaymentDunningAttempt;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentDunningAttemptRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.DunningService;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
public class DunningServiceImpl implements DunningService {

    private static final int[] RETRY_DAYS = {1, 3, 7};
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final PaymentDunningAttemptRepository dunningRepo;
    private final PaymentIntentRepository intentRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final PawapayDepositRepository depositRepo;

    @Lazy private final PaymentService paymentService;
    @Lazy private final SubscriptionService subscriptionService;

    public DunningServiceImpl(
            PaymentDunningAttemptRepository dunningRepo,
            PaymentIntentRepository intentRepo,
            PaymentTransactionRepository transactionRepo,
            PawapayDepositRepository depositRepo,
            @Lazy PaymentService paymentService,
            @Lazy SubscriptionService subscriptionService) {
        this.dunningRepo = dunningRepo;
        this.intentRepo = intentRepo;
        this.transactionRepo = transactionRepo;
        this.depositRepo = depositRepo;
        this.paymentService = paymentService;
        this.subscriptionService = subscriptionService;
    }

    @Override
    @Transactional
    public void scheduleForFailedIntent(PaymentIntent intent, String subscriptionNumber) {
        boolean alreadyScheduled = dunningRepo.existsByPaymentIntentIdAndStatusIn(
                intent.getId(), List.of(DunningAttemptStatus.PENDING, DunningAttemptStatus.EXECUTING));
        if (alreadyScheduled) {
            log.debug("Dunning already scheduled for intent {}", intent.getIntentNumber());
            return;
        }
        Instant base = Instant.now();
        Instant extendedExpiry = base.plus(RETRY_DAYS[RETRY_DAYS.length - 1] + 1, ChronoUnit.DAYS);
        intent.setExpiresAt(extendedExpiry);
        intentRepo.save(intent);

        for (int i = 0; i < RETRY_DAYS.length; i++) {
            dunningRepo.save(PaymentDunningAttempt.builder()
                    .paymentIntentId(intent.getId())
                    .subscriptionNumber(subscriptionNumber)
                    .attemptNumber(i + 1)
                    .scheduledAt(base.plus(RETRY_DAYS[i], ChronoUnit.DAYS))
                    .status(DunningAttemptStatus.PENDING)
                    .build());
        }
        log.info("Scheduled {} dunning attempts for intent {}, expiry extended to {}",
                RETRY_DAYS.length, intent.getIntentNumber(), extendedExpiry);
    }

    @Override
    public void executeAttempt(PaymentDunningAttempt attempt) {
        attempt.setStatus(DunningAttemptStatus.EXECUTING);
        attempt.setExecutedAt(Instant.now());
        dunningRepo.save(attempt);

        try {
            PaymentIntent intent = intentRepo.findById(attempt.getPaymentIntentId())
                    .orElseThrow(() -> new IllegalStateException("Intent not found: " + attempt.getPaymentIntentId()));

            PawapayDeposit originalDeposit = depositRepo
                    .findLatestByPaymentIntentId(intent.getId())
                    .orElseThrow(() -> new IllegalStateException("No PawaPay deposit found for intent " + intent.getIntentNumber()));

            String newTxNumber = paymentService.retryMobileMoneyDeposit(
                    intent.getIntentNumber(),
                    originalDeposit.getPhoneNumber(),
                    originalDeposit.getProvider());

            attempt.setStatus(DunningAttemptStatus.SUCCEEDED);
            attempt.setNewTransactionNumber(newTxNumber);
            dunningRepo.save(attempt);
            log.info("Dunning attempt {} succeeded for intent {}, new tx {}",
                    attempt.getAttemptNumber(), intent.getIntentNumber(), newTxNumber);

        } catch (Exception e) {
            log.warn("Dunning attempt {} failed for intent {}: {}",
                    attempt.getAttemptNumber(), attempt.getPaymentIntentId(), e.getMessage());
            attempt.setStatus(DunningAttemptStatus.FAILED);
            attempt.setFailureReason(e.getMessage());
            dunningRepo.save(attempt);

            checkExhaustedAndSuspend(attempt);
        }
    }

    private void checkExhaustedAndSuspend(PaymentDunningAttempt lastFailed) {
        if (!StringUtils.hasText(lastFailed.getSubscriptionNumber())) return;

        List<PaymentDunningAttempt> all = dunningRepo
                .findAllByPaymentIntentIdOrderByAttemptNumber(lastFailed.getPaymentIntentId());

        boolean allDone = all.stream().noneMatch(a -> a.getStatus() == DunningAttemptStatus.PENDING);
        boolean anySucceeded = all.stream().anyMatch(a -> a.getStatus() == DunningAttemptStatus.SUCCEEDED);

        if (allDone && !anySucceeded) {
            try {
                subscriptionService.suspend(lastFailed.getSubscriptionNumber(),
                        new SubscriptionStatusChangeRequest(
                                "Suspension automatique après " + all.size() + " tentatives de recouvrement échouées",
                                SYSTEM_ACTOR,
                                Boolean.FALSE));
                log.info("Suspended subscription {} after exhausted dunning for intent {}",
                        lastFailed.getSubscriptionNumber(), lastFailed.getPaymentIntentId());
            } catch (Exception e) {
                log.error("Failed to suspend subscription {} after dunning exhaustion: {}",
                        lastFailed.getSubscriptionNumber(), e.getMessage());
            }
        }
    }
}