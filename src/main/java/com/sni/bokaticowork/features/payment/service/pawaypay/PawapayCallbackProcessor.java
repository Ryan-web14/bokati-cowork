package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.fasterxml.jackson.databind.JsonNode;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PawapayCallbackProcessor {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentIntentRepository intentRepository;
    private final PaymentAllocationService allocationService;
    private final OutboxService outboxService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final PawapayDepositService depositService;
    private final PawapayDepositRepository depositRepository;

    public void process(PawapayCallbackPayload payload) {
        String depositId = payload.depositId();
        if (depositId == null) {
            log.warn("Received PawaPay callback with no depositId");
            return;
        }

        PaymentTransaction transaction = resolveTransaction(payload);

        if (transaction == null) {
            log.warn("PawaPay callback received for unknown depositId={}, clientReferenceId={}",
                    depositId, payload.clientReferenceId());
            return;
        }

        String status = payload.status() == null ? "" : payload.status().trim().toUpperCase();
        if ("COMPLETED".equals(status) || "SUCCESSFUL".equals(status) || "SUCCEEDED".equals(status)) {
            handleCompleted(transaction, depositId);
            depositService.markCompleted(depositId, payload);
        } else if ("FAILED".equals(status) || "REJECTED".equals(status) || "EXPIRED".equals(status)) {
            if (isTerminal(transaction.getStatus())) {
                log.info("Ignoring terminal PawaPay failure callback for depositId {} because transaction is already {}",
                        depositId, transaction.getStatus());
                return;
            }
            handleFailed(transaction, payload);
        } else {
            log.warn("Unexpected PawaPay callback status '{}' for depositId {}", payload.status(), depositId);
        }
    }

    private PaymentTransaction resolveTransaction(PawapayCallbackPayload payload) {
        return resolveTransaction(payload.depositId(), payload.clientReferenceId())
                .or(() -> findLatestCallbackCandidate(payload))
                .map(transaction -> ensureProviderReference(transaction, payload.depositId()))
                .orElse(null);
    }

    private Optional<PaymentTransaction> resolveTransaction(String depositId, String clientReferenceId) {
        return transactionRepository.findByProviderReference(depositId)
                .or(() -> findDepositTransaction(depositId, clientReferenceId))
                .or(() -> clientReferenceId == null || clientReferenceId.isBlank()
                        ? Optional.empty()
                        : transactionRepository.findByTransactionNumber(clientReferenceId.trim()));
    }

    private Optional<PaymentTransaction> findLatestCallbackCandidate(PawapayCallbackPayload payload) {
        String phoneNumber = textAt(payload.payer(), "accountDetails", "phoneNumber");
        String provider = textAt(payload.payer(), "accountDetails", "provider");
        if (clean(phoneNumber) == null || clean(provider) == null) {
            return Optional.empty();
        }
        return depositRepository.findLatestMatchingCallback(
                        clean(phoneNumber),
                        clean(provider),
                        clean(payload.currency()),
                        clean(payload.amount())
                )
                .map(deposit -> deposit.getPaymentTransaction());
    }

    private String textAt(JsonNode root, String first, String second) {
        if (root == null || root.isNull()) {
            return null;
        }
        JsonNode firstNode = root.get(first);
        if (firstNode == null || firstNode.isNull()) {
            return null;
        }
        JsonNode secondNode = firstNode.get(second);
        return secondNode != null && secondNode.isTextual() ? secondNode.asText() : null;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Optional<PaymentTransaction> findDepositTransaction(String depositId, String clientReferenceId) {
        Optional<PaymentTransaction> byDepositId = depositRepository.findByDepositId(depositId)
                .map(deposit -> deposit.getPaymentTransaction());
        if (byDepositId.isPresent()) {
            return byDepositId;
        }
        if (clientReferenceId == null || clientReferenceId.isBlank()) {
            return Optional.empty();
        }
        return depositRepository.findByClientReferenceId(clientReferenceId.trim())
                .map(deposit -> deposit.getPaymentTransaction());
    }

    private PaymentTransaction ensureProviderReference(PaymentTransaction transaction, String depositId) {
        if (transaction.getProviderReference() == null || transaction.getProviderReference().isBlank()) {
            transaction.setProviderReference(depositId);
            return transactionRepository.save(transaction);
        }
        return transaction;
    }

    private void handleCompleted(PaymentTransaction transaction, String depositId) {
        if (transaction.getStatus() != PaymentTransactionStatus.SUCCEEDED) {
            Instant paidAt = Instant.now();
            transaction.setStatus(PaymentTransactionStatus.SUCCEEDED);
            transaction.setPaidAt(paidAt);
            if (transaction.getReceiptNumber() == null || transaction.getReceiptNumber().isBlank()) {
                transaction.setReceiptNumber(sequenceGenerator.next("receipt"));
            }
            transaction.setReceiptIssuedAt(transaction.getReceiptIssuedAt() == null ? paidAt : transaction.getReceiptIssuedAt());
            if (transaction.getProviderReference() == null || transaction.getProviderReference().isBlank()) {
                transaction.setProviderReference(depositId);
            }
            transactionRepository.save(transaction);
            try {
                allocationService.allocateIfBillingDocument(transaction);
            } catch (Exception ex) {
                // On ne fait pas echouer l'encaissement : l'argent est bien recu et la transaction
                // doit passer SUCCEEDED. Mais l'echec d'imputation laissait auparavant la facture
                // impayee sans aucun rattrapage - le client avait paye et relancait quand meme.
                // On le transforme en evenement outbox rejouable avec backoff.
                log.error("Echec d'imputation du paiement {} · rattrapage programme via l'outbox",
                        transaction.getTransactionNumber(), ex);
                outboxService.publish(
                        "PAYMENT_ALLOCATION_RETRY",
                        "PAYMENT",
                        transaction.getTransactionNumber(),
                        java.util.Map.of(
                                "transactionNumber", transaction.getTransactionNumber(),
                                "reason", String.valueOf(ex.getMessage()))
                );
            }
            outboxService.publish(
                    "PAYMENT_TRANSACTION_WORKFLOW",
                    "PAYMENT",
                    transaction.getTransactionNumber(),
                    java.util.Map.of("transactionNumber", transaction.getTransactionNumber(), "status", PaymentTransactionStatus.SUCCEEDED.name())
            );
        }

        reconcileIntent(transaction);

        log.info("PawaPay deposit completed · transaction={}, depositId={}",
                transaction.getTransactionNumber(), depositId);
    }

    private void reconcileIntent(PaymentTransaction transaction) {
        // flush first · sumSucceededAmountByPaymentIntentId is a native query and Hibernate
        // does not auto-flush before native queries, so without this the just-saved
        // SUCCEEDED transaction is invisible to the SUM and the intent lands on PENDING.
        transactionRepository.flush();
        PaymentIntent intent = transaction.getPaymentIntent();
        BigDecimal paidAmount = transactionRepository.sumSucceededAmountByPaymentIntentId(intent.getId());
        BigDecimal processingAmount = transactionRepository.sumProcessingAmountByPaymentIntentId(intent.getId());
        if (paidAmount.compareTo(intent.getAmount()) >= 0) {
            intent.setStatus(PaymentIntentStatus.SUCCEEDED);
        } else if (processingAmount.signum() > 0) {
            intent.setStatus(PaymentIntentStatus.PROCESSING);
        } else {
            intent.setStatus(PaymentIntentStatus.PENDING);
        }
        intentRepository.save(intent);
    }

    private void handleFailed(PaymentTransaction transaction, PawapayCallbackPayload payload) {
        String reason = payload.failureReason() != null
                ? payload.failureReason().toString()
                : "Payment failed at operator";

        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        transactionRepository.save(transaction);
        depositService.markFailed(transaction.getProviderReference(), payload, reason);

        reconcileIntent(transaction);

        // A failed mobile money deposit invalidates the payment intent right away · no
        // retry/dunning. If the intent is not otherwise settled (nothing paid, nothing in
        // flight), cancel it so it stops sitting in PENDING and is no longer payable.
        PaymentIntent intent = transaction.getPaymentIntent();
        if (intent.getStatus() == PaymentIntentStatus.PENDING) {
            intent.setStatus(PaymentIntentStatus.CANCELLED);
            intentRepository.save(intent);
            log.info("Cancelled payment intent {} after mobile money deposit failure",
                    intent.getIntentNumber());
        }

        log.info("PawaPay deposit failed · transaction={}, depositId={}, reason={}",
                transaction.getTransactionNumber(), transaction.getProviderReference(), reason);
    }

    private boolean isTerminal(PaymentTransactionStatus status) {
        return status == PaymentTransactionStatus.SUCCEEDED
                || status == PaymentTransactionStatus.FAILED
                || status == PaymentTransactionStatus.CANCELLED
                || status == PaymentTransactionStatus.REVERSED
                || status == PaymentTransactionStatus.REFUNDED;
    }
}
