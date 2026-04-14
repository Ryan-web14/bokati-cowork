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

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final OutboxNotificationMailService mailService;
    private final OutboxRecipientResolver recipientResolver;

    @Override
    public boolean supports(OutboxEvent event) {
        return "DOCUMENT".equalsIgnoreCase(event.getAggregateType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);
        DocumentOwnerType ownerType = enumValue(payload, "ownerType", DocumentOwnerType.class);
        Long ownerId = longValue(payload, "ownerId");
        String documentCode = textValue(payload, "documentCode");
        String status = textValue(payload, "status");

        OutboxRecipientResolver.Recipient recipient = recipientResolver.resolveByOwnerId(ownerType, ownerId);
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            log.info("No document recipient resolved for event {} and document {}", event.getEventType(), documentCode);
            return;
        }

        mailService.sendDocumentNotification(recipient.email(), java.util.Map.of(
                "recipientName", StringUtils.hasText(recipient.displayName()) ? recipient.displayName() : "client",
                "documentCode", documentCode,
                "eventType", event.getEventType(),
                "status", status == null ? "N/A" : status
        ));
    }

    private JsonNode readPayload(OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read document outbox payload", ex);
        }
    }

    private String textValue(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private Long longValue(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node.asLong();
    }

    private <T extends Enum<T>> T enumValue(JsonNode payload, String field, Class<T> type) {
        String value = textValue(payload, field);
        return value == null ? null : Enum.valueOf(type, value);
    }
}
