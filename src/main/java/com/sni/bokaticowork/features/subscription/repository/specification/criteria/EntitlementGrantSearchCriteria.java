package com.sni.bokaticowork.features.subscription.repository.specification.criteria;

import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class EntitlementGrantSearchCriteria {
    private SubscriberType ownerType;
    private String ownerCode;
    private String entitlementCode;
    private EntitlementGrantStatus status;
    private Instant validAt;
}
