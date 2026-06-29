package com.sni.bokaticowork.features.portal.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientSubscribeRequest(
        @NotBlank String planCode,
        @NotNull BillingCycle billingCycle,
        Boolean autoRenew
) {
}
