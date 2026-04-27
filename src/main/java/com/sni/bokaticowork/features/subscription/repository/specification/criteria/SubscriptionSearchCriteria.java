package com.sni.bokaticowork.features.subscription.repository.specification.criteria;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SubscriptionSearchCriteria {
    private SubscriberType subscriberType;
    private String subscriberCode;
    private String planCode;
    private SubscriptionStatus status;
    private LocalDate nextBillingBefore;
}
