package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSubscriptionRequest(
        @NotBlank String planCode,
        String planVersionId,
        BillingCycle billingCycle,
        @NotNull SubscriberType subscriberType,
        @NotBlank String subscriberCode,
        @NotNull LocalDate startDate,
        Boolean autoRenew,
        String metadataJson,
        Boolean autoActivate
) {
}
