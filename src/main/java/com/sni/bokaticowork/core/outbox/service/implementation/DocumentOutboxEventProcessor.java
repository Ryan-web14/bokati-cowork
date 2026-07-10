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
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentOutboxEventProcessor implements OutboxEventProcessor {

    private static final Set<DocumentOwnerType> CLIENT_OWNER_TYPES = Set.of(
            DocumentOwnerType.CUSTOMER,
            DocumentOwnerType.MEMBER,
            DocumentOwnerType.BUSINESS
    );

    private final ObjectMapper objectMapper;
    private final OutboxNotificationMailService mailService;
    private final OutboxRecipientResolver recipientResolver;

    @Override
    public boolean supports(OutboxEvent event) {
        return "DOCUMENT".equalsIgnoreCase(event.getAggregateType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload        = readPayload(event);
        String documentTypeCode = textValue(payload, "documentTypeCode");
        if ("CONTRACT_DRAFT".equalsIgnoreCase(documentTypeCode)) {
            log.info("Skipping notification for CONTRACT_DRAFT document event={}", event.getEventType());
            return;
        }

        DocumentOwnerType ownerType = enumValue(payload, "ownerType", DocumentOwnerType.class);
        Long ownerId        = longValue(payload, "ownerId");
        String documentCode = textValue(payload, "documentCode");
        String status       = textValue(payload, "status");

        log.info("Processing DOCUMENT outbox event={} ownerType={} ownerId={} documentCode={}",
                event.getEventType(), ownerType, ownerId, documentCode);

        if (ownerType == null || ownerId == null) {
            log.warn("DOCUMENT event {} missing ownerType/ownerId · skipping notification", event.getEventType());
            return;
        }

        if (CLIENT_OWNER_TYPES.contains(ownerType)) {
            log.info("Skipping document email notification for client ownerType={} event={}", ownerType, event.getEventType());
            return;
        }

        OutboxRecipientResolver.Recipient recipient;
        try {
            recipient = recipientResolver.resolveByOwnerId(ownerType, ownerId);
        } catch (Exception ex) {
            log.warn("Could not resolve recipient for DOCUMENT event={} ownerType={} ownerId={}: {}",
                    event.getEventType(), ownerType, ownerId, ex.getMessage());
            return;
        }
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            log.warn("No email address for DOCUMENT event={} ownerType={} ownerId={} · skipping notification",
                    event.getEventType(), ownerType, ownerId);
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", StringUtils.hasText(recipient.displayName()) ? recipient.displayName() : "client");
        vars.put("documentCode",  documentCode != null ? documentCode : "");
        vars.put("eventType",     event.getEventType());
        vars.put("status",        status != null ? status : "N/A");

        try {
            mailService.sendDocumentNotification(recipient.email(), vars);
            log.info("Document notification sent to {} for event={}", recipient.email(), event.getEventType());
        } catch (Exception ex) {
            log.warn("Failed to send document notification for event={} to {}: {}",
                    event.getEventType(), recipient.email(), ex.getMessage());
        }
    }

    private JsonNode readPayload(OutboxEvent event) {
        try { return objectMapper.readTree(event.getPayload()); }
        catch (Exception ex) { throw new IllegalStateException("Unable to read document outbox payload", ex); }
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
        if (value == null) return null;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ex) {
            log.warn("Unknown enum value '{}' for {}", value, type.getSimpleName());
            return null;
        }
    }
}
