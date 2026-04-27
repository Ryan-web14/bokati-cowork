package com.sni.bokaticowork.features.subscription.promotion.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RedeemPromotionRequest(
        @NotNull SubscriberType subscriberType,
        @NotBlank String subscriberCode,
        String subscriptionNumber
) {
}
