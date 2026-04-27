package com.sni.bokaticowork.features.subscription.notification.service;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.notification.dto.CreateSubscriptionNotificationRequest;
import com.sni.bokaticowork.features.subscription.notification.dto.SubscriptionNotificationResponse;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationStatus;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.springframework.data.domain.Pageable;

public interface SubscriptionNotificationService {

    SubscriptionNotificationResponse queue(CreateSubscriptionNotificationRequest request);

    int dispatchDue(int limit);

    int cancel(String notificationNumber);

    PaginatedResponse<SubscriptionNotificationResponse> list(String subscriptionNumber,
                                                             SubscriberType ownerType,
                                                             String ownerCode,
                                                             SubscriptionNotificationType notificationType,
                                                             SubscriptionNotificationStatus status,
                                                             Pageable pageable);
}
