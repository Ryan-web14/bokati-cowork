package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.service.support.PaymentTransactionWorkflowEvent;
import com.sni.bokaticowork.features.payment.service.support.PaymentTransactionWorkflowProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentTransactionWorkflowOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final PaymentTransactionWorkflowProcessor processor;

    public PaymentTransactionWorkflowOutboxEventProcessor(
            ObjectMapper objectMapper,
            @Lazy PaymentTransactionWorkflowProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "PAYMENT_TRANSACTION_WORKFLOW".equals(event.getEventType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);
        String transactionNumber = payload.path("transactionNumber").asText(null);
        String statusRaw = payload.path("status").asText(null);

        if (transactionNumber == null || statusRaw == null) {
            throw new IllegalStateException(
                    "PAYMENT_TRANSACTION_WORKFLOW payload missing transactionNumber or status: " + event.getPayload());
        }

        PaymentTransactionStatus status;
        try {
            status = PaymentTransactionStatus.valueOf(statusRaw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unknown PaymentTransactionStatus: " + statusRaw, ex);
        }

        log.info("Processing payment workflow via outbox: transaction={} status={}", transactionNumber, status);
        processor.process(new PaymentTransactionWorkflowEvent(transactionNumber, status));
    }

    private JsonNode readPayload(OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse PAYMENT_TRANSACTION_WORKFLOW payload", ex);
        }
    }
}
