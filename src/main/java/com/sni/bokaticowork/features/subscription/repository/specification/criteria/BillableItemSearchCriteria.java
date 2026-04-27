package com.sni.bokaticowork.features.subscription.repository.specification.criteria;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BillableItemSearchCriteria {
    private BillableItemStatus status;
    private SubscriberType subscriberType;
    private String subscriberCode;
    private String sourceType;
    private String sourceId;
}
