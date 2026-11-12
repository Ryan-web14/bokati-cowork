package com.sni.bokaticowork.features.portal.notification.service;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import com.sni.bokaticowork.features.notification.service.support.MemberNotificationEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberInAppNotifier {

    private final NotificationService notificationService;

    private MemberPreferences memberPreferences;

    @Autowired(required = false)
    void setMemberPreferences(MemberPreferences memberPreferences) {
        this.memberPreferences = memberPreferences;
    }

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
        if (!allowedByMember(eventType, recipientEmail, recipientCode, payload)) {
            log.info("Notification {} {} non creee dans l'application pour {} : preference du membre",
                    eventType, aggregateId, recipientEmail);
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

    /**
     * Le membre a-t-il coupe cet evenement dans l'application ? Seuls les evenements de preference
     * se coupent ; une relance, un contrat ou une demande d'approbation part toujours.
     */
    private boolean allowedByMember(String eventType, String recipientEmail, String recipientCode, Map<String, Object> payload) {
        if (memberPreferences == null) {
            return true;
        }
        Optional<EventType> event = MemberNotificationEvents.of(eventType, payload);
        if (event.isEmpty()) {
            return true;
        }
        com.sni.bokaticowork.core.enums.NotificationChannel channel = com.sni.bokaticowork.core.enums.NotificationChannel.IN_APP;
        try {
            return memberPreferences.memberByEmail(recipientEmail)
                    .map(member -> memberPreferences.allows(member, event.get(), channel))
                    .orElseGet(() -> memberPreferences.allows(recipientCode, event.get(), channel));
        } catch (RuntimeException ex) {
            log.debug("Preference du membre illisible pour {}, notification maintenue : {}", recipientEmail, ex.getMessage());
            return true;
        }
    }
}
