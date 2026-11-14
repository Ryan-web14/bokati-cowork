package com.sni.bokaticowork.features.billing.element.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Traite l'annonce d'un paiement recu sur une facture : effets sur les elements qu'elle regle (lot R2). */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElementSettlementOutboxProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final ElementSettlementService settlements;

    @Override
    public boolean supports(OutboxEvent event) {
        return ElementSettlementPublisher.EVENT_TYPE.equals(event.getEventType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Annonce de reglement illisible : " + event.getPayload(), ex);
        }
        String documentNumber = payload.path("documentNumber").asText(event.getAggregateId());
        String reason = payload.path("reason").asText("Paiement sur " + documentNumber);
        settlements.settle(documentNumber, reason).forEach(outcome ->
                log.info("Reglement de {} : {} {} {} ({})", documentNumber, outcome.type(), outcome.code(), outcome.result(), outcome.detail()));
    }
}
