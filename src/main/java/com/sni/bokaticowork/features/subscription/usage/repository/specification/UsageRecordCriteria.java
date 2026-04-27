package com.sni.bokaticowork.features.subscription.usage.repository.specification;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.usage.enums.UsageRecordStatus;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UsageRecordCriteria {
    private SubscriberType ownerType;
    private String ownerCode;
    private String entitlementCode;
    private String referenceType;
    private String referenceId;
    private UsageRecordStatus status;
}
