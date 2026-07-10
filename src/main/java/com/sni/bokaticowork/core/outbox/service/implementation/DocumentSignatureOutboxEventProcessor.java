package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.communication.mailService.interfaces.OutboxNotificationMailService;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSignatureOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final OutboxNotificationMailService mailService;

    @Override
    public boolean supports(OutboxEvent event) {
        return "DOCUMENT_SIGNATURE".equalsIgnoreCase(event.getAggregateType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);
        String signerEmail = textValue(payload, "signerEmail");
        String signerName  = textValue(payload, "signerName");
        String documentCode = textValue(payload, "documentCode");
        String signatureStatus = textValue(payload, "signatureStatus");

        log.info("Processing DOCUMENT_SIGNATURE event={} documentCode={} signer={}",
                event.getEventType(), documentCode, signerEmail);

        if (!StringUtils.hasText(signerEmail)) {
            log.warn("DOCUMENT_SIGNATURE event {} has no signerEmail · skipping", event.getEventType());
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", StringUtils.hasText(signerName) ? signerName : "signataire");
        vars.put("documentCode", documentCode != null ? documentCode : "");
        vars.put("eventType", event.getEventType());
        vars.put("status", signatureStatus != null ? signatureStatus : "N/A");

        try {
            mailService.sendDocumentNotification(signerEmail, vars);
            log.info("Document signature notification sent to {} for event={}", signerEmail, event.getEventType());
        } catch (Exception ex) {
            log.warn("Failed to send document signature notification for event={} to {}: {}",
                    event.getEventType(), signerEmail, ex.getMessage());
        }
    }

    private JsonNode readPayload(OutboxEvent event) {
        try { return objectMapper.readTree(event.getPayload()); }
        catch (Exception ex) { throw new IllegalStateException("Unable to read document signature outbox payload", ex); }
    }

    private String textValue(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }
}
