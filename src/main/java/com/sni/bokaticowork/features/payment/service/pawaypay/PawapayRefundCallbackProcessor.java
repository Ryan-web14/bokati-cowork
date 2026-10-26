package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PawapayRefundCallbackProcessor {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentIntentRepository intentRepository;
    private final OutboxService outboxService;
    private final com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService allocationService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public void process(PawapayRefundCallbackPayload payload) {
        if (payload.refundId() == null) {
            log.warn("PawaPay refund callback received with no refundId");
            return;
        }

        PaymentTransaction transaction = transactionRepository.findByProviderReference(payload.refundId())
                .orElse(null);

        if (transaction == null) {
            log.warn("PawaPay refund callback for unknown refundId: {}", payload.refundId());
            return;
        }

        if (isTerminal(transaction.getStatus())) {
            log.info("Ignoring duplicate PawaPay refund callback for refundId {} · already in terminal state {}",
                    payload.refundId(), transaction.getStatus());
            return;
        }

        if ("COMPLETED".equals(payload.status())) {
            PaymentIntent intent = transaction.getPaymentIntent();
            PaymentTransaction original = originalOf(transaction)
                    .or(() -> transactionRepository.findSucceededByPaymentIntentId(intent.getId()))
                    .orElse(null);
            // La facture n'est rouverte qu'ici, quand PawaPay confirme · voir PaymentServiceImpl.
            if (original != null && transaction.getAmount() != null) {
                allocationService.reverseAllocations(original, transaction.getAmount());
            }
            BigDecimal projectedRefundedTotal = original == null
                    ? BigDecimal.ZERO
                    : refundedTotal(original.getTransactionNumber()).add(transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount());

            transaction.setStatus(PaymentTransactionStatus.REFUNDED);
            transaction.setPaidAt(Instant.now());
            transactionRepository.save(transaction);

            if (original != null && projectedRefundedTotal.compareTo(original.getAmount()) >= 0) {
                original.setStatus(PaymentTransactionStatus.REFUNDED);
                transactionRepository.save(original);
                intent.setStatus(PaymentIntentStatus.REFUNDED);
                intentRepository.save(intent);
            }

            outboxService.publish(
                    "PAYMENT_TRANSACTION_WORKFLOW",
                    "PAYMENT",
                    transaction.getTransactionNumber(),
                    java.util.Map.of("transactionNumber", transaction.getTransactionNumber(), "status", PaymentTransactionStatus.REFUNDED.name())
            );
            log.info("PawaPay refund completed · transaction={}, refundId={}",
                    transaction.getTransactionNumber(), payload.refundId());
        } else if ("FAILED".equals(payload.status())) {
            String reason = payload.failureReason() != null
                    ? payload.failureReason().toString()
                    : "Refund failed at operator";
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setFailureReason(reason);
            transactionRepository.save(transaction);
            log.info("PawaPay refund failed · transaction={}, refundId={}, reason={}",
                    transaction.getTransactionNumber(), payload.refundId(), reason);
        } else {
            log.warn("Unexpected PawaPay refund callback status '{}' for refundId {}",
                    payload.status(), payload.refundId());
        }
    }

    /** La transaction remboursee, par le lien que porte le remboursement · plus sur que « le dernier paiement reussi ». */
    private java.util.Optional<PaymentTransaction> originalOf(PaymentTransaction refund) {
        try {
            String metadata = refund.getMetadataJson();
            if (metadata == null || metadata.isBlank()) {
                return java.util.Optional.empty();
            }
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(metadata).get("originalTransactionNumber");
            return node == null || node.isNull()
                    ? java.util.Optional.empty()
                    : transactionRepository.findByTransactionNumber(node.asText());
        } catch (java.io.IOException ex) {
            return java.util.Optional.empty();
        }
    }

    private BigDecimal refundedTotal(String transactionNumber) {
        return transactionRepository.findAllByOriginalTransactionNumber(transactionNumber).stream()
                .filter(t -> t.getStatus() == PaymentTransactionStatus.REFUNDED || t.getStatus() == PaymentTransactionStatus.REVERSED)
                .map(PaymentTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isTerminal(PaymentTransactionStatus status) {
        return status == PaymentTransactionStatus.REFUNDED
                || status == PaymentTransactionStatus.REVERSED
                || status == PaymentTransactionStatus.FAILED
                || status == PaymentTransactionStatus.CANCELLED;
    }
}