package com.sni.bokaticowork.features.portal.notification.service;

import com.sni.bokaticowork.core.event.WebSocketTopics;
import com.sni.bokaticowork.core.event.dto.AdminAlertEvent;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

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
        try {
            messagingTemplate.convertAndSend(WebSocketTopics.ADMIN_ALERTS, new AdminAlertEvent(
                    alertType, module, title, message, severity, payloadJson, Instant.now()
            ));
        } catch (Exception ex) {
            log.warn("Failed to broadcast admin alert {}/{}", module, alertType, ex);
        }
    }
}
