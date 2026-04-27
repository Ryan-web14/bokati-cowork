package com.sni.bokaticowork.features.subscription.promotion.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;

import java.time.Instant;

public record CouponRedemptionResponse(
        String promotionCode,
        SubscriberType subscriberType,
        String subscriberCode,
        String subscriptionNumber,
        Instant redeemedAt
) {
}
