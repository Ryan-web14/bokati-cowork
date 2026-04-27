package com.sni.bokaticowork.features.subscription.notification.dto;

import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationChannel;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationStatus;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.time.Instant;

public record SubscriptionNotificationResponse(
        String notificationNumber,
        String subscriptionNumber,
        SubscriberType ownerType,
        String ownerCode,
        SubscriptionNotificationType notificationType,
        SubscriptionNotificationChannel channel,
        String recipient,
        String subject,
        String body,
        String payloadJson,
        Instant scheduledAt,
        Instant dispatchedAt,
        SubscriptionNotificationStatus status,
        Integer attemptCount,
        String errorMessage
) {
}
