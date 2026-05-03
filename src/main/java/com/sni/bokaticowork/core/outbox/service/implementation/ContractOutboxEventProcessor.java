package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.communication.mailService.interfaces.OutboxNotificationMailService;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class  ContractOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final OutboxNotificationMailService mailService;
    private final OutboxRecipientResolver recipientResolver;

    @Override
    public boolean supports(OutboxEvent event) {
        return "CONTRACT".equalsIgnoreCase(event.getAggregateType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);
        DocumentOwnerType ownerType = enumValue(payload, "ownerType", DocumentOwnerType.class);
        String ownerCode = textValue(payload, "ownerCode");
        String documentCode = textValue(payload, "documentCode");
        String templateCode = textValue(payload, "templateCode");

        OutboxRecipientResolver.Recipient recipient = recipientResolver.resolveByOwnerCode(ownerType, ownerCode);
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            log.info("No contract recipient resolved for event {} and owner {}", event.getEventType(), ownerCode);
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", StringUtils.hasText(recipient.displayName()) ? recipient.displayName() : "client");
        variables.put("templateCode", templateCode);
        variables.put("documentCode", documentCode);
        variables.put("eventType", event.getEventType());
        mailService.sendContractNotification(recipient.email(), variables);
    }

    private JsonNode readPayload(OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read contract outbox payload", ex);
        }
    }

    private String textValue(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private <T extends Enum<T>> T enumValue(JsonNode payload, String field, Class<T> type) {
        String value = textValue(payload, field);
        return value == null ? null : Enum.valueOf(type, value);
    }
}
