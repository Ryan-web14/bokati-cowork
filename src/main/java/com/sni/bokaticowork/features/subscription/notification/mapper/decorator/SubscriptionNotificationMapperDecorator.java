package com.sni.bokaticowork.features.subscription.notification.mapper.decorator;

import com.sni.bokaticowork.features.subscription.notification.dto.CreateSubscriptionNotificationRequest;
import com.sni.bokaticowork.features.subscription.notification.dto.SubscriptionNotificationResponse;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationStatus;
import com.sni.bokaticowork.features.subscription.notification.mapper.interfaces.SubscriptionNotificationMapper;
import com.sni.bokaticowork.features.subscription.notification.model.SubscriptionNotificationOutbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Component
public abstract class SubscriptionNotificationMapperDecorator implements SubscriptionNotificationMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionNotificationMapper delegate;

    @Override
    public SubscriptionNotificationOutbox toEntity(CreateSubscriptionNotificationRequest request) {
        SubscriptionNotificationOutbox notification = delegate.toEntity(request);
        notification.setOwnerCode(trim(request.ownerCode()));
        notification.setRecipient(request.recipient().trim());
        notification.setSubject(trim(request.subject()));
        notification.setBody(trim(request.body()));
        notification.setPayloadJson(trim(request.payloadJson()));
        notification.setScheduledAt(request.scheduledAt() == null ? Instant.now() : request.scheduledAt());
        notification.setStatus(SubscriptionNotificationStatus.PENDING);
        notification.setAttemptCount(0);
        return notification;
    }

    @Override
    public SubscriptionNotificationResponse toResponse(SubscriptionNotificationOutbox notification) {
        return new SubscriptionNotificationResponse(
                notification.getNotificationNumber(),
                notification.getSubscription() == null ? null : notification.getSubscription().getSubscriptionNumber(),
                notification.getOwnerType(),
                notification.getOwnerCode(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getBody(),
                notification.getPayloadJson(),
                notification.getScheduledAt(),
                notification.getDispatchedAt(),
                notification.getStatus(),
                notification.getAttemptCount(),
                notification.getErrorMessage()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
