package com.sni.bokaticowork.features.subscription.notification.dto;

import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationChannel;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateSubscriptionNotificationRequest(
        String subscriptionNumber,
        SubscriberType ownerType,
        String ownerCode,
        @NotNull SubscriptionNotificationType notificationType,
        @NotNull SubscriptionNotificationChannel channel,
        @NotBlank String recipient,
        String subject,
        String body,
        String payloadJson,
        Instant scheduledAt
) {
}
