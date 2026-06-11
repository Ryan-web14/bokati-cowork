package com.sni.bokaticowork.features.notification.service.interfaces;

import com.sni.bokaticowork.features.notification.dto.request.NotificationTemplateRequest;
import com.sni.bokaticowork.features.notification.dto.request.PublishNotificationEventRequest;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.response.NotificationDispatchResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationTemplateResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    CompletableFuture<NotificationDispatchResponse> send(SendNotificationRequest request);

    void publish(PublishNotificationEventRequest request);

    Page<NotificationMessageResponse> list(NotificationDeliveryStatus status,
                                           NotificationChannel channel,
                                           String eventType,
                                           String recipientEmail,
                                           String search,
                                           Pageable pageable);

    NotificationMessageResponse retry(String notificationNumber);

    List<NotificationMessageResponse> listUnread(String recipientEmail, int limit);

    NotificationMessageResponse markAsRead(String notificationNumber);

    int markAllRead(String recipientEmail);

    NotificationTemplateResponse upsertTemplate(NotificationTemplateRequest request);

    List<NotificationTemplateResponse> listTemplates(NotificationChannel channel, Boolean active);

    int processPending(int batchSize);
}
