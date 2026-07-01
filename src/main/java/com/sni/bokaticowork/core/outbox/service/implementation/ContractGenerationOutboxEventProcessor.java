package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.subscription.subscription.service.support.ContractGenerationEvent;
import com.sni.bokaticowork.features.subscription.subscription.service.support.ContractGenerationProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContractGenerationOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final ContractGenerationProcessor contractGenerationProcessor;

    public ContractGenerationOutboxEventProcessor(ObjectMapper objectMapper,
                                                  @Lazy ContractGenerationProcessor contractGenerationProcessor) {
        this.objectMapper = objectMapper;
        this.contractGenerationProcessor = contractGenerationProcessor;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "CONTRACT_GENERATION_REQUESTED".equals(event.getEventType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);
        String sourceType = payload.path("sourceType").asText(null);
        long   sourceId   = payload.path("sourceId").asLong(0);

        if (sourceType == null || sourceId == 0) {
            throw new IllegalStateException(
                    "CONTRACT_GENERATION_REQUESTED payload missing sourceType or sourceId: " + event.getPayload());
        }
        log.info("Generating contract via outbox: sourceType={} sourceId={}", sourceType, sourceId);
        contractGenerationProcessor.process(new ContractGenerationEvent(sourceType, sourceId));
    }

    private JsonNode readPayload(OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse CONTRACT_GENERATION_REQUESTED payload", ex);
        }
    }
}
