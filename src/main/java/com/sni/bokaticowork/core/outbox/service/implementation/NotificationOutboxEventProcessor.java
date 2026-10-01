package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.notification.service.support.NotificationDispatchSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

// Broad fallback processor · claims any event from a known aggregate type.
// Event types with dedicated processors are explicitly excluded so findFirst() in
// OutboxServiceImpl always routes workflow events to their specific processor first.
// @Order(Integer.MAX_VALUE) reinforces this as a last-resort bean when injected as a List.
@Order(Integer.MAX_VALUE)
@Component
@RequiredArgsConstructor
public class NotificationOutboxEventProcessor implements OutboxEventProcessor {

    private static final Set<String> SUPPORTED_AGGREGATES = Set.of(
            "NOTIFICATION",
            "SUBSCRIPTION",
            "SUBSCRIPTION_PASS",
            "PAYMENT",
            "BILLING",
            "BILLING_DOCUMENT",
            "INVOICE",
            "QUOTE",
            "WALLET",
            "DOMICILIATION",
            "SUBSCRIPTION_QUOTE",
            "CASH_REGISTER",
            "INVENTORY",
            "ADMIN"
    );

    // Event types handled by dedicated processors · must never be claimed here.
    private static final Set<String> DEDICATED_EVENT_TYPES = Set.of(
            "PAYMENT_TRANSACTION_WORKFLOW",
            "CONTRACT_GENERATION_REQUESTED",
            "CONTRACT_RENEWAL_AMENDMENT_REQUESTED"
    );

    private final ObjectMapper objectMapper;
    private final NotificationDispatchSupport dispatchSupport;

    @Override
    public boolean supports(OutboxEvent event) {
        if (event.getEventType() != null && DEDICATED_EVENT_TYPES.contains(event.getEventType())) {
            return false;
        }
        return (event.getEventType() != null && event.getEventType().startsWith("NOTIFICATION_"))
                || (event.getAggregateType() != null && SUPPORTED_AGGREGATES.contains(event.getAggregateType().toUpperCase()));
    }

    @Override
    public void process(OutboxEvent event) {
        dispatchSupport.createForEvent(
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                readPayload(event)
        );
    }

    private Map<String, Object> readPayload(OutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read notification outbox payload", ex);
        }
    }
}
