package com.sni.bokaticowork.features.portal.notification.service;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientNotificationService {

    private static final int MAX_UNREAD_LIMIT = 50;

    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<NotificationMessageResponse> listUnread(Member member, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_UNREAD_LIMIT));
        return notificationService.listUnread(member.getEmail(), safeLimit);
    }

    @Transactional
    public NotificationMessageResponse markAsRead(Member member, String notificationNumber) {
        NotificationMessageResponse notification = notificationService.markAsRead(notificationNumber);
        verifyRecipient(member, notification);
        return notification;
    }

    @Transactional
    public int markAllRead(Member member) {
        return notificationService.markAllRead(member.getEmail());
    }

    private void verifyRecipient(Member member, NotificationMessageResponse notification) {
        if (!member.getEmail().equalsIgnoreCase(notification.recipientEmail())) {
            throw new com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException(
                    "Notification not found");
        }
    }
}
