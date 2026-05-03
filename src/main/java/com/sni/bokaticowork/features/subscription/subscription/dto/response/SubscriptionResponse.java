package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionResponse(
        String subscriptionNumber,
        SubscriberType subscriberType,
        String subscriberCode,
        String subscriberName,
        String planCode,
        String planName,
        Integer planVersion,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        LocalDate nextBillingDate,
        Boolean autoRenew,
        BillingCycle billingCycle,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Boolean cancelAtPeriodEnd,
        Instant pausedAt,
        LocalDate pauseUntil,
        String contractCode,
        Instant createdAt,
        Instant updatedAt
) {
}
