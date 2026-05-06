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
public class KycOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final OutboxNotificationMailService mailService;
    private final OutboxRecipientResolver recipientResolver;

    @Override
    public boolean supports(OutboxEvent event) {
        return "KYC_CASE".equalsIgnoreCase(event.getAggregateType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);
        DocumentOwnerType ownerType = enumValue(payload, "ownerType", DocumentOwnerType.class);
        Long ownerId = longValue(payload, "ownerId");
        String caseCode = textValue(payload, "kycCaseCode");
        String status   = textValue(payload, "status");

        log.info("Processing KYC outbox event={} case={} ownerType={} ownerId={}",
                event.getEventType(), caseCode, ownerType, ownerId);

        if (ownerType == null || ownerId == null) {
            log.warn("KYC event {} missing ownerType or ownerId — skipping", event.getEventType());
            return;
        }

        OutboxRecipientResolver.Recipient recipient = recipientResolver.resolveByOwnerId(ownerType, ownerId);
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            log.warn("No KYC recipient for event={} ownerType={} ownerId={}", event.getEventType(), ownerType, ownerId);
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", StringUtils.hasText(recipient.displayName()) ? recipient.displayName() : "client");
        vars.put("kycCaseCode",   caseCode != null ? caseCode : "—");
        vars.put("eventType",     event.getEventType());
        vars.put("status",        status != null ? status : "N/A");

        // Lance une exception si l'envoi échoue → outbox marque FAILED → retry automatique
        mailService.sendKycNotification(recipient.email(), vars);
        log.info("KYC notification sent to {} for event={}", recipient.email(), event.getEventType());
    }

    private JsonNode readPayload(OutboxEvent event) {
        try { return objectMapper.readTree(event.getPayload()); }
        catch (Exception ex) { throw new IllegalStateException("Unable to read KYC outbox payload", ex); }
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
