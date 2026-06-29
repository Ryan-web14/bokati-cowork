package com.sni.bokaticowork.features.portal.notification.service;

import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberInAppNotifier {

    private final NotificationService notificationService;

    public void notify(String eventType,
                       String aggregateType,
                       String aggregateId,
                       String recipientEmail,
                       String recipientName,
                       String recipientCode,
                       String subject,
                       Map<String, Object> payload) {
        if (!StringUtils.hasText(recipientEmail)) {
            return;
        }
        try {
            notificationService.send(new SendNotificationRequest(
                    eventType,
                    aggregateType,
                    aggregateId,
                    NotificationChannel.IN_APP,
                    NotificationRecipientType.MEMBER,
                    recipientCode,
                    recipientEmail.trim().toLowerCase(),
                    recipientName,
                    subject,
                    eventType,
                    null,
                    payload,
                    null
            ));
        } catch (Exception ex) {
            log.warn("Failed to create IN_APP notification {} for {}", eventType, recipientEmail, ex);
        }
    }
}
