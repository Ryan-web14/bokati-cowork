package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.support.PaymentTransactionWorkflowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PawapayRefundCallbackProcessor {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentIntentRepository intentRepository;
    private final ApplicationEventPublisher eventPublisher;

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
            log.info("Ignoring duplicate PawaPay refund callback for refundId {} — already in terminal state {}",
                    payload.refundId(), transaction.getStatus());
            return;
        }

        if ("COMPLETED".equals(payload.status())) {
            transaction.setStatus(PaymentTransactionStatus.REFUNDED);
            transaction.setPaidAt(Instant.now());
            transactionRepository.save(transaction);

            PaymentIntent intent = transaction.getPaymentIntent();
            transactionRepository.findSucceededByPaymentIntentId(intent.getId())
                    .ifPresent(original -> {
                        original.setStatus(PaymentTransactionStatus.REFUNDED);
                        transactionRepository.save(original);
                    });
            intent.setStatus(PaymentIntentStatus.REFUNDED);
            intentRepository.save(intent);

            eventPublisher.publishEvent(new PaymentTransactionWorkflowEvent(
                    transaction.getTransactionNumber(), PaymentTransactionStatus.REFUNDED));
            log.info("PawaPay refund completed — transaction={}, refundId={}",
                    transaction.getTransactionNumber(), payload.refundId());
        } else if ("FAILED".equals(payload.status())) {
            String reason = payload.failureReason() != null
                    ? payload.failureReason().toString()
                    : "Refund failed at operator";
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setFailureReason(reason);
            transactionRepository.save(transaction);
            log.info("PawaPay refund failed — transaction={}, refundId={}, reason={}",
                    transaction.getTransactionNumber(), payload.refundId(), reason);
        } else {
            log.warn("Unexpected PawaPay refund callback status '{}' for refundId {}",
                    payload.status(), payload.refundId());
        }
    }

    private boolean isTerminal(PaymentTransactionStatus status) {
        return status == PaymentTransactionStatus.REFUNDED
                || status == PaymentTransactionStatus.REVERSED
                || status == PaymentTransactionStatus.FAILED
                || status == PaymentTransactionStatus.CANCELLED;
    }
}