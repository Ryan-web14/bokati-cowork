package com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement;

import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.math.BigDecimal;

public record EntitlementQuotaThresholdEvent(
        String grantNumber,
        String subscriptionNumber,
        SubscriberType ownerType,
        String ownerCode,
        String entitlementCode,
        BigDecimal quantityGranted,
        BigDecimal quantityRemaining,
        int threshold,
        SubscriptionNotificationType notificationType
) {}
