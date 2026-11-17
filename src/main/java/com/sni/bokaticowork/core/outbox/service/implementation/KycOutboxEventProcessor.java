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
public class KycOutboxEventProcessor implements OutboxEventProcessor {

    // We only notify the client for meaningful, action-oriented milestones:
    //  - KYC_CASE_CREATED             → a case was opened manually by an admin
    //  - KYC_CASE_APPROVED            → the case was validated
    //  - KYC_CASE_REJECTED / _CORRECTION_REQUESTED → a change is requested
    //  - KYC_INCOMPLETE_REMINDER      → friendly reminder for a still-incomplete case
    // Automatic case creation (KYC_CASE_AUTO_CREATED) is intentionally silent.
    private static final Set<String> NOTIFIABLE_EVENTS = Set.of(
            "KYC_CASE_CREATED",
            "KYC_CASE_APPROVED",
            "KYC_CASE_REJECTED",
            "KYC_CASE_CORRECTION_REQUESTED",
            "KYC_INCOMPLETE_REMINDER",
            "KYC_CASE_LEVEL_CHANGED",
            "KYC_CASE_REQUIREMENT_ADDED",
            "KYC_DOCUMENT_CORRECTION_REQUESTED",
            "KYC_RENEWAL_REQUIRED"
    );

    private final ObjectMapper objectMapper;
    private final OutboxNotificationMailService mailService;
    private final OutboxRecipientResolver recipientResolver;
    private com.sni.bokaticowork.features.document.kyc.service.support.KycWhatsAppNotifier whatsAppNotifier;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setWhatsAppNotifier(com.sni.bokaticowork.features.document.kyc.service.support.KycWhatsAppNotifier whatsAppNotifier) {
        this.whatsAppNotifier = whatsAppNotifier;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        // Lot KG3 : tous les evenements de dossier ont un processeur. Les autres (soumission,
        // affectation, note, recalcul...) sont consignes, sans courriel : avant, faute de
        // processeur, ils etaient retentes puis abandonnes (DEAD).
        return "KYC_CASE".equalsIgnoreCase(event.getAggregateType());
    }

    @Override
    public void process(OutboxEvent event) {
        if (!NOTIFIABLE_EVENTS.contains(event.getEventType())) {
            log.debug("KYC event {} recorded without notification", event.getEventType());
            return;
        }
        JsonNode payload = readPayload(event);
        DocumentOwnerType ownerType = enumValue(payload, "ownerType", DocumentOwnerType.class);
        Long ownerId = longValue(payload, "ownerId");
        String caseCode = textValue(payload, "kycCaseCode");
        String status   = textValue(payload, "status");

        log.info("Processing KYC outbox event={} case={} ownerType={} ownerId={}",
                event.getEventType(), caseCode, ownerType, ownerId);

        if (ownerType == null || ownerId == null) {
            log.warn("KYC event {} missing ownerType or ownerId - skipping", event.getEventType());
            return;
        }

        OutboxRecipientResolver.Recipient recipient = recipientResolver.resolveByOwnerId(ownerType, ownerId);
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            log.warn("No KYC recipient for event={} ownerType={} ownerId={}", event.getEventType(), ownerType, ownerId);
            return;
        }

        String subject = resolveSubject(event.getEventType(), caseCode);

        Map<String, Object> vars = new HashMap<>();
        vars.put("recipientName", StringUtils.hasText(recipient.displayName()) ? recipient.displayName() : "client");
        vars.put("kycCaseCode",   caseCode != null ? caseCode : "");
        vars.put("eventType",     event.getEventType());
        vars.put("eventLabel",    resolveEventLabel(event.getEventType()));
        vars.put("status",        status != null ? status : "N/A");
        // Lot KG1 : niveau KYC change, pieces a fournir.
        vars.put("level", textValue(payload, "level"));
        vars.put("reason", textValue(payload, "reason"));
        java.util.List<String> names = new java.util.ArrayList<>();
        JsonNode missing = payload.get("missingDocumentNames");
        if (missing != null && missing.isArray()) {
            missing.forEach(node -> names.add(node.asText()));
        }
        vars.put("missingDocuments", names);
        vars.put("documentTypeName", textValue(payload, "documentTypeName"));
        JsonNode required = payload.get("required");
        vars.put("required", required == null || required.isNull() ? null : required.asBoolean());
        if (vars.get("reason") == null) {
            vars.put("reason", textValue(payload, "comment"));
        }

        // Lot KG3 : un echec de mise en file remonte, la boite d'envoi reessaie (avant, il etait avale).
        mailService.sendKycNotification(recipient.email(), subject, vars);
        log.info("KYC notification queued for {} event={}", recipient.email(), event.getEventType());
        // Lot KG7 : pieces demandees ou a corriger, aussi par WhatsApp (modele approuve, consentement du membre).
        if (whatsAppNotifier != null) {
            java.util.List<String> pieces = new java.util.ArrayList<>(names);
            String single = textValue(payload, "documentTypeName");
            if (pieces.isEmpty() && StringUtils.hasText(single)) {
                pieces.add(single);
            }
            whatsAppNotifier.notify(event.getEventType(), ownerType, ownerId, caseCode, pieces);
        }
    }

    private String resolveSubject(String eventType, String caseCode) {
        String ref = caseCode != null ? " - " + caseCode : "";
        return switch (eventType) {
            case "KYC_CASE_CREATED"      -> "Votre dossier KYC est ouvert" + ref;
            case "KYC_CASE_APPROVED"     -> "Bonne nouvelle : votre dossier KYC est validé" + ref;
            case "KYC_CASE_REJECTED" -> "Votre dossier KYC n'a pas été retenu" + ref;
            case "KYC_CASE_CORRECTION_REQUESTED" -> "Une petite mise à jour est nécessaire sur votre dossier KYC" + ref;
            case "KYC_INCOMPLETE_REMINDER" -> "Petit rappel : votre dossier KYC vous attend" + ref;
            case "KYC_CASE_LEVEL_CHANGED" -> "Quelques pièces complémentaires pour votre dossier KYC" + ref;
            case "KYC_CASE_REQUIREMENT_ADDED" -> "Une pièce de plus pour votre dossier KYC" + ref;
            case "KYC_DOCUMENT_CORRECTION_REQUESTED" -> "Une pièce de votre dossier KYC est à corriger" + ref;
            case "KYC_RENEWAL_REQUIRED" -> "Votre dossier KYC est à renouveler" + ref;
            default                      -> "Votre dossier KYC" + ref;
        };
    }

    private String resolveEventLabel(String eventType) {
        return switch (eventType) {
            case "KYC_CASE_CREATED"      -> "Dossier ouvert";
            case "KYC_CASE_APPROVED"     -> "Dossier validé";
            case "KYC_CASE_REJECTED" -> "Dossier non retenu";
            case "KYC_CASE_CORRECTION_REQUESTED" -> "Mise à jour demandée";
            case "KYC_INCOMPLETE_REMINDER" -> "Rappel";
            case "KYC_CASE_LEVEL_CHANGED" -> "Pièces complémentaires";
            case "KYC_CASE_REQUIREMENT_ADDED" -> "Pièce demandée";
            case "KYC_DOCUMENT_CORRECTION_REQUESTED" -> "Pièce à corriger";
            case "KYC_RENEWAL_REQUIRED" -> "Renouvellement";
            default                      -> "Notification";
        };
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
