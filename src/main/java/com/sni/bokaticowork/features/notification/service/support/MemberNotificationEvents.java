package com.sni.bokaticowork.features.notification.service.support;

import com.sni.bokaticowork.core.enums.EventType;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * L'evenement de preference qui correspond a un message destine a un membre.
 *
 * <p>Seuls les messages listes ici se coupent par les preferences du membre : confirmation,
 * annulation, rappel et echec de reservation, facture emise, paiement recu ou echoue. Tout le
 * reste (relances d'impayes, securite, contrats, conformite, messages a l'administration ou a une
 * organisation) rend vide et part toujours.</p>
 */
public final class MemberNotificationEvents {

    private MemberNotificationEvents() {
    }

    public static Optional<EventType> of(String eventName) {
        return of(eventName, null);
    }

    /** Le payload sert a distinguer un devis (jamais coupe) d'une facture. */
    public static Optional<EventType> of(String eventName, Map<String, Object> payload) {
        if (!StringUtils.hasText(eventName)) {
            return Optional.empty();
        }
        String name = eventName.trim().toUpperCase(Locale.ROOT);
        EventType event = switch (name) {
            case "BOOKING_CREATED", "BOOKING_CONFIRMED", "BOOKING_APPROVED" -> EventType.BOOKING_CONFIRMED;
            case "BOOKING_CANCELLED" -> EventType.BOOKING_CANCELLED;
            case "BOOKING_REMINDER" -> EventType.BOOKING_REMINDER;
            case "BOOKING_REJECTED", "BOOKING_HOLD_EXPIRED", "BOOKING_FAILED" -> EventType.BOOKING_FAILED;
            case "BILLING_DOCUMENT_ISSUED", "INVOICE_ISSUED" -> isQuote(payload) ? null : EventType.INVOICE_ISSUED;
            case "PAYMENT_RECEIVED", "MARKETPLACE_ORDER_PAID" -> EventType.PAYMENT_RECEIVED;
            case "PAYMENT_FAILED", "MOBILE_MONEY_DEPOSIT_FAILED" -> EventType.PAYMENT_FAILED;
            default -> null;
        };
        return Optional.ofNullable(event);
    }

    private static boolean isQuote(Map<String, Object> payload) {
        if (payload == null) {
            return false;
        }
        Object type = payload.get("documentType");
        return type != null && "QUOTE".equalsIgnoreCase(type.toString().trim());
    }
}
