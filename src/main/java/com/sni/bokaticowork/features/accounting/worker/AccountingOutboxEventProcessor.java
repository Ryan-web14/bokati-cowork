package com.sni.bokaticowork.features.accounting.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.accounting.AccountingEventPublisher;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingEngine;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Reception des evenements comptables publies par l'outbox (lot F2) : enregistrement dans le
 * registre d'idempotence. La comptabilisation elle-meme est faite par {@link PostingWorker}, pour
 * qu'un probleme comptable ne fasse jamais rejouer l'outbox.
 */
@Component
public class AccountingOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final PostingEngine engine;

    public AccountingOutboxEventProcessor(ObjectMapper objectMapper, @Lazy PostingEngine engine) {
        this.objectMapper = objectMapper;
        this.engine = engine;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return AccountingEventPublisher.OUTBOX_EVENT_TYPE.equals(event.getEventType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(event.getPayload());
        } catch (Exception exception) {
            throw new IllegalStateException("Evenement comptable illisible : " + exception.getMessage(), exception);
        }
        String date = payload.path("date").asText(null);
        engine.receive(payload.path("eventType").asText(null), payload.path("reference").asText(null),
                payload.path("family").asText(null), date == null || date.isBlank() ? null : LocalDate.parse(date),
                payload.path("data").toString(), false);
    }
}
