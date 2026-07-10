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
public class ContractOutboxEventProcessor implements OutboxEventProcessor {

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

        if ("CONTRACT_SIGNING_REQUESTED".equals(event.getEventType())) {
            processSigningRequest(event, payload);
            return;
        }

        DocumentOwnerType ownerType = enumValue(payload, "ownerType", DocumentOwnerType.class);
        String ownerCode     = textValue(payload, "ownerCode");
        String documentCode  = textValue(payload, "documentCode");
        String templateCode  = textValue(payload, "templateCode");

        log.info("Processing CONTRACT outbox event={} ownerType={} ownerCode={} documentCode={}",
                event.getEventType(), ownerType, ownerCode, documentCode);

        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            log.warn("CONTRACT event {} missing ownerType/ownerCode · skipping notification", event.getEventType());
            return;
        }

        OutboxRecipientResolver.Recipient recipient;
        try {
            recipient = recipientResolver.resolveByOwnerCode(ownerType, ownerCode);
        } catch (Exception ex) {
            log.warn("Could not resolve recipient for CONTRACT event={} ownerType={} ownerCode={}: {}",
                    event.getEventType(), ownerType, ownerCode, ex.getMessage());
            return;
        }
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            log.warn("No email address for CONTRACT event={} ownerType={} ownerCode={} · skipping notification",
                    event.getEventType(), ownerType, ownerCode);
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", StringUtils.hasText(recipient.displayName()) ? recipient.displayName() : "client");
        vars.put("templateCode",  templateCode);
        vars.put("documentCode",  documentCode);
        vars.put("eventType",     event.getEventType());
        vars.put("contractCode",  textValue(payload, "contractCode"));
        vars.put("endDate",       textValue(payload, "endDate"));
        vars.put("daysUntilExpiry", textValue(payload, "daysUntilExpiry"));

        try {
            mailService.sendContractNotification(recipient.email(), vars);
            log.info("Contract notification sent to {} for event={}", recipient.email(), event.getEventType());
        } catch (Exception ex) {
            log.warn("Failed to send contract notification for event={} to {}: {}",
                    event.getEventType(), recipient.email(), ex.getMessage());
        }
    }

    private void processSigningRequest(OutboxEvent event, JsonNode payload) {
        String signerEmail   = textValue(payload, "signerEmail");
        String signerName    = textValue(payload, "signerName");
        String contractCode  = textValue(payload, "contractCode");
        String contractTitle = textValue(payload, "contractTitle");
        String signingUrl    = textValue(payload, "signingUrl");
        String expiresAt     = textValue(payload, "expiresAt");

        log.info("Processing CONTRACT_SIGNING_REQUESTED contract={} signer={}", contractCode, signerEmail);

        if (!StringUtils.hasText(signerEmail)) {
            log.warn("CONTRACT_SIGNING_REQUESTED for contract={} has no signerEmail · skipping notification", contractCode);
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", StringUtils.hasText(signerName) ? signerName : "signataire");
        vars.put("contractCode", contractCode);
        vars.put("contractTitle", contractTitle);
        vars.put("signingUrl", signingUrl);
        vars.put("expiresAt", expiresAt);
        vars.put("eventType", event.getEventType());

        try {
            mailService.sendContractSigningNotification(signerEmail, "Signature requise · " + contractTitle, vars);
            log.info("Signing request notification sent to {} for contract={}", signerEmail, contractCode);
        } catch (Exception ex) {
            log.warn("Failed to send signing notification for contract={} to {}: {}",
                    contractCode, signerEmail, ex.getMessage());
        }
    }

    private JsonNode readPayload(OutboxEvent event) {
        try { return objectMapper.readTree(event.getPayload()); }
        catch (Exception ex) { throw new IllegalStateException("Unable to read contract outbox payload", ex); }
    }

    private String textValue(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
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
