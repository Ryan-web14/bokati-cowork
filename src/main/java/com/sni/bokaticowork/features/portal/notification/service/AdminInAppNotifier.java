package com.sni.bokaticowork.features.portal.notification.service;

import com.sni.bokaticowork.core.event.WebSocketTopics;
import com.sni.bokaticowork.core.event.dto.AdminAlertEvent;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInAppNotifier {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public void notify(String eventType,
                       String aggregateType,
                       String aggregateId,
                       String adminEmail,
                       String subject,
                       Map<String, Object> payload) {
        if (!StringUtils.hasText(adminEmail)) {
            return;
        }
        try {
            notificationService.send(new SendNotificationRequest(
                    eventType,
                    aggregateType,
                    aggregateId,
                    NotificationChannel.IN_APP,
                    NotificationRecipientType.ADMIN,
                    null,
                    adminEmail.trim().toLowerCase(),
                    "Admin",
                    subject,
                    eventType,
                    null,
                    payload,
                    null
            ));
        } catch (Exception ex) {
            log.warn("Failed to create admin IN_APP notification {} for {}", eventType, adminEmail, ex);
        }
    }

    public void broadcastAlert(String alertType, String module, String title,
                               String message, String severity, String payloadJson) {
        AdminAlertEvent alert = new AdminAlertEvent(alertType, module, title, message, severity, payloadJson, Instant.now());
        try {
            messagingTemplate.convertAndSend(WebSocketTopics.ADMIN_ALERTS, alert);
        } catch (MessageDeliveryException ex) {
            // Courtier pas encore connecte (premiers passages des taches planifiees apres un
            // demarrage) : l'alerte attend la connexion au lieu d'etre perdue.
            if (pendingAlerts.size() < MAX_PENDING) {
                pendingAlerts.add(alert);
            }
            log.info("Admin alert {}/{} kept until the message broker is available ({} pending)", module, alertType, pendingAlerts.size());
        } catch (Exception ex) {
            log.warn("Failed to broadcast admin alert {}/{}", module, alertType, ex);
        }
    }

    /** Envoi des alertes en attente des que le courtier de messages est disponible. */
    @EventListener
    public void onBrokerAvailability(BrokerAvailabilityEvent event) {
        if (!event.isBrokerAvailable()) {
            return;
        }
        AdminAlertEvent alert;
        while ((alert = pendingAlerts.poll()) != null) {
            try {
                messagingTemplate.convertAndSend(WebSocketTopics.ADMIN_ALERTS, alert);
            } catch (Exception ex) {
                log.warn("Failed to broadcast pending admin alert {}/{}", alert.module(), alert.alertType(), ex);
                return;
            }
        }
    }

    private static final int MAX_PENDING = 50;
    private final Queue<AdminAlertEvent> pendingAlerts = new ConcurrentLinkedQueue<>();
}
