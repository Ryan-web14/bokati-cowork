package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.notification.service.support.NotificationDispatchSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationOutboxEventProcessor implements OutboxEventProcessor {

    private static final Set<String> SUPPORTED_AGGREGATES = Set.of(
            "NOTIFICATION",
            "BOOKING",
            "SUBSCRIPTION",
            "SUBSCRIPTION_PASS",
            "PAYMENT",
            "BILLING",
            "INVOICE",
            "QUOTE",
            "WALLET",
            "CASH_REGISTER",
            "INVENTORY",
            "ADMIN"
    );

    private final ObjectMapper objectMapper;
    private final NotificationDispatchSupport dispatchSupport;

    @Override
    public boolean supports(OutboxEvent event) {
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
