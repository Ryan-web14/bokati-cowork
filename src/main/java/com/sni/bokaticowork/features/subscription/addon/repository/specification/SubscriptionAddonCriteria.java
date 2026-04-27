package com.sni.bokaticowork.features.subscription.addon.repository.specification;

import com.sni.bokaticowork.features.subscription.addon.enums.SubscriptionAddonStatus;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SubscriptionAddonCriteria {
    private String subscriptionNumber;
    private String planCode;
    private SubscriptionAddonStatus status;
}
