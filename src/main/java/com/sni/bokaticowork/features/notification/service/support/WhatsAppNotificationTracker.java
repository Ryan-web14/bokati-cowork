package com.sni.bokaticowork.features.notification.service.support;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppStatusEvent;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.model.NotificationMessage;
import com.sni.bokaticowork.features.notification.repository.NotificationMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Reflet des messages WhatsApp dans notification_message (canal WHATSAPP) : la notification est
 * creee a l'envoi, puis passe a DELIVERED ou FAILED selon Infobip ; la lecture renseigne readAt.
 */
@Component
@RequiredArgsConstructor
public class WhatsAppNotificationTracker {

    private final NotificationMessageRepository messages;

    @EventListener
    public void onStatus(WhatsAppStatusEvent event) {
        if (event.notificationNumber() == null) {
            return;
        }
        NotificationMessage message = messages.findByNotificationNumber(event.notificationNumber()).orElseGet(() -> NotificationMessage.builder()
                .notificationNumber(event.notificationNumber())
                .eventType(event.purpose() == null ? "WHATSAPP" : event.purpose())
                .aggregateType("WHATSAPP")
                .aggregateId(event.reference())
                .channel(NotificationChannel.WHATSAPP)
                .recipientCode(event.reference())
                .recipientPhone(event.phone())
                .subject(event.templateName() == null ? "Message WhatsApp" : "Modele " + event.templateName())
                .templateCode(event.templateName())
                .templateName(event.templateName())
                .status(NotificationDeliveryStatus.SENT)
                .attempts(1)
                .availableAt(Instant.now())
                .createdAt(Instant.now())
                .build());
        if (event.failed()) {
            message.setStatus(NotificationDeliveryStatus.FAILED);
            message.setLastError(event.error() != null ? event.error() : event.statusName() + " : " + event.description());
        } else if ("DELIVERED".equals(event.statusGroup()) || event.seenAt() != null) {
            message.setStatus(NotificationDeliveryStatus.DELIVERED);
            message.setLastError(null);
        } else if (message.getStatus() != NotificationDeliveryStatus.DELIVERED) {
            message.setStatus(NotificationDeliveryStatus.SENT);
        }
        if (message.getSentAt() == null) {
            message.setSentAt(event.sentAt() == null ? Instant.now() : event.sentAt());
        }
        if (event.seenAt() != null) {
            message.setReadAt(event.seenAt());
        }
        message.setUpdatedAt(Instant.now());
        messages.save(message);
    }
}
