package com.sni.bokaticowork.features.portal.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.portal.notification.dto.ClientNotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientNotificationFormatter {

    private final ObjectMapper objectMapper;

    public ClientNotificationResponse format(NotificationMessageResponse notification) {
        Map<String, Object> payload = parsePayload(notification.payloadJson());
        String title = notification.subject();
        String message = buildMessage(notification.eventType(), payload);
        return new ClientNotificationResponse(
                notification.notificationNumber(),
                title,
                message,
                notification.createdAt(),
                notification.readAt() != null
        );
    }

    private String buildMessage(String eventType, Map<String, Object> payload) {
        if (eventType == null) return "";
        return switch (eventType) {
            case "BOOKING_CREATED", "BOOKING_CONFIRMED" -> bookingMessage(
                    "Votre reservation %s pour %s a ete confirmee.",
                    payload);
            case "BOOKING_CANCELLED" -> bookingMessage(
                    "Votre reservation %s pour %s a ete annulee.",
                    payload);
            case "BOOKING_COMPLETED" -> bookingMessage(
                    "Votre reservation %s pour %s est terminee.",
                    payload);
            case "BOOKING_REJECTED" -> bookingMessage(
                    "Votre reservation %s pour %s a ete rejetee.",
                    payload);
            case "BOOKING_STARTED" -> bookingMessage(
                    "Votre reservation %s pour %s a demarre.",
                    payload);
            case "BOOKING_CHECKED_IN" -> bookingMessage(
                    "Check-in enregistre pour la reservation %s (%s).",
                    payload);
            case "BOOKING_CHECKED_OUT" -> bookingMessage(
                    "Check-out enregistre pour la reservation %s (%s).",
                    payload);
            case "BOOKING_NO_SHOW" -> bookingMessage(
                    "Absence enregistree pour la reservation %s (%s).",
                    payload);
            case "SUBSCRIPTION_CREATED" -> subscriptionMessage(
                    "Votre abonnement %s au plan %s a ete cree.",
                    payload);
            case "SUBSCRIPTION_ACTIVATED" -> subscriptionMessage(
                    "Votre abonnement %s au plan %s est maintenant actif.",
                    payload);
            case "SUBSCRIPTION_CANCELLED" -> subscriptionMessage(
                    "Votre abonnement %s au plan %s a ete annule.",
                    payload);
            case "SUBSCRIPTION_RENEWED" -> subscriptionMessage(
                    "Votre abonnement %s au plan %s a ete renouvele.",
                    payload);
            case "SUBSCRIPTION_EXPIRED" -> subscriptionMessage(
                    "Votre abonnement %s au plan %s a expire.",
                    payload);
            case "CONTRACT_SIGNING_REQUESTED" -> String.format(
                    "Un nouveau contrat \"%s\" est pret pour signature.",
                    str(payload, "contractTitle"));
            case "CONTRACT_SIGNED_VIA_ESIGN" -> String.format(
                    "Le contrat \"%s\" a ete signe avec succes.",
                    str(payload, "contractTitle"));
            case "BILLING_DOCUMENT_ISSUED" -> String.format(
                    "La facture %s (%s) a ete emise.",
                    str(payload, "documentNumber"),
                    str(payload, "documentType").toLowerCase());
            default -> "";
        };
    }

    private String bookingMessage(String template, Map<String, Object> payload) {
        return String.format(template,
                str(payload, "bookingNumber"),
                str(payload, "resourceName"));
    }

    private String subscriptionMessage(String template, Map<String, Object> payload) {
        return String.format(template,
                str(payload, "subscriptionNumber"),
                str(payload, "planName"));
    }

    private String str(Map<String, Object> payload, String key) {
        if (payload == null) return "";
        Object value = payload.get(key);
        return value != null ? value.toString() : "";
    }

    private Map<String, Object> parsePayload(String json) {
        if (!StringUtils.hasText(json)) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to parse notification payload", ex);
            return Map.of();
        }
    }
}
