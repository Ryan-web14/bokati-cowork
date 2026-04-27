package com.sni.bokaticowork.features.subscription.change.repository.specification;

import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeStatus;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SubscriptionChangeCriteria {
    private String subscriptionNumber;
    private SubscriptionChangeType changeType;
    private SubscriptionChangeStatus status;
}
