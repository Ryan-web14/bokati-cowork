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
public class KycDocumentOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final OutboxNotificationMailService mailService;
    private final OutboxRecipientResolver recipientResolver;

    @Override
    public boolean supports(OutboxEvent event) {
        return "KYC_DOCUMENT".equalsIgnoreCase(event.getAggregateType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);
        DocumentOwnerType ownerType  = enumValue(payload, "ownerType", DocumentOwnerType.class);
        Long ownerId                 = longValue(payload, "ownerId");
        String documentType          = textValue(payload, "documentType");
        String expiryDate            = textValue(payload, "expiryDate");
        String kycCaseCode           = textValue(payload, "kycCaseCode");

        log.info("Processing KYC_DOCUMENT outbox event={} ownerType={} ownerId={}",
                event.getEventType(), ownerType, ownerId);

        if (ownerType == null || ownerId == null) {
            log.warn("KYC_DOCUMENT event {} missing ownerType/ownerId · skipping", event.getEventType());
            return;
        }

        OutboxRecipientResolver.Recipient recipient = recipientResolver.resolveByOwnerId(ownerType, ownerId);
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            log.warn("No KYC document recipient for event={} ownerType={} ownerId={}",
                    event.getEventType(), ownerType, ownerId);
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("ownerName",    StringUtils.hasText(recipient.displayName()) ? recipient.displayName() : "client");
        vars.put("documentType", documentType != null ? documentType : "document");
        vars.put("expiryDate",   expiryDate  != null ? expiryDate  : "");
        vars.put("kycCaseCode",  kycCaseCode != null ? kycCaseCode : "");
        vars.put("eventType",    event.getEventType());

        try {
            mailService.sendKycDocumentNotification(recipient.email(), vars);
            log.info("KYC document notification sent to {} for event={}", recipient.email(), event.getEventType());
        } catch (Exception ex) {
            log.warn("Failed to send KYC document notification for event={} to {}: {}",
                    event.getEventType(), recipient.email(), ex.getMessage());
        }
    }

    private JsonNode readPayload(OutboxEvent event) {
        try { return objectMapper.readTree(event.getPayload()); }
        catch (Exception ex) { throw new IllegalStateException("Unable to read KYC document outbox payload", ex); }
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
